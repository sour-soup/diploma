import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:bim_bim_app/constants/constants.dart';
import 'package:bim_bim_app/services/api_client.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

class ChatPage extends StatefulWidget {
  final String chatName;
  final int chatId;
  final String avatar;

  const ChatPage(
      {super.key,
      required this.chatName,
      required this.chatId,
      required this.avatar});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  List<Map<String, dynamic>> _messages = [];
  final TextEditingController _controller = TextEditingController();
  final ApiClient _apiClient = ApiClient();
  bool isLoading = true;
  Timer? _updateTimer;

  @override
  void initState() {
    super.initState();
    _loadMessages();

    _updateTimer = Timer.periodic(const Duration(seconds: 3), (timer) {
      _loadMessages();
    });
  }

  @override
  void dispose() {
    _updateTimer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _uploadImage(XFile imageFile) async {
    try {
      final file = await http.MultipartFile.fromPath(
        'image',
        imageFile.path,
      );

      final response = await _apiClient.uploadMultipart(
        endpoint: '$baseUrl/chat/${widget.chatId}/uploadImage',
        files: [file],
        fields: {'type': 'image'}
      );

      print(response);
      print(response.statusCode);
      print(response.headers);

      if (response.statusCode != 200) {
        throw Exception('Failed to upload image');
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString()}')),
      );
    }
  }

  Future<void> _pickImage() async {
    try {
      final picker = ImagePicker();
      final pickedFile = await picker.pickImage(source: ImageSource.gallery);

      if (pickedFile != null) {
        await _uploadImage(pickedFile);
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString()}')),
      );
    }
  }

  Future<void> _loadMessages() async {
    try {
      final response = await _apiClient.get('$baseUrl/chat/${widget.chatId}/messages');

      print(response.body);

      if (response.statusCode != 200) {
        throw Exception('Failed to load messages');
      }

      final decodedBody = utf8.decode(response.bodyBytes);
      final List<dynamic> data = json.decode(decodedBody);

      setState(() {
        _messages = data.map((message) {
          return {
            'isMe': message['isMe'],
            'content': message['content'],
            'image': message['image'],
          };
        }).toList();
        isLoading = false;
      });
    } catch (e) {
      setState(() {
        isLoading = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString()}')),
      );
    }
  }

  Future<void> _sendMessage() async {
    if (_controller.text.trim().isEmpty) return;

    try {
      final response = await _apiClient.post(
        '$baseUrl/chat/${widget.chatId}/messages',
        body: _controller.text.trim()
      );

      if (response.statusCode != 200) {
        throw Exception('Failed to send message');
      }

      setState(() {
        _messages.add({
          'isMe': true,
          'content': _controller.text.trim(),
        });
        _controller.clear();
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: ${e.toString()}')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: Text(widget.chatName),
      ),
      body: Column(
        children: [
          Expanded(
            child: isLoading
                ? const Center(
                    child: CircularProgressIndicator(
                      valueColor:
                          AlwaysStoppedAnimation<Color>(Color(0xFF64FFDA)),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final message = _messages[index];
                      final isMe = message['isMe'];
                      return Align(
                        alignment:
                            isMe ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          decoration: BoxDecoration(
                            color: isMe
                                ? const Color(0xFF1F2937)
                                : const Color(0xFF2D2D34),
                            borderRadius: BorderRadius.circular(16),
                            boxShadow: [
                              BoxShadow(
                                color: isMe
                                    ? const Color(0xFF64FFDA).withOpacity(0.1)
                                    : const Color(0xFFBB86FC).withOpacity(0.1),
                                blurRadius: 10,
                                offset: const Offset(2, 4),
                              ),
                            ],
                          ),
                          margin: const EdgeInsets.symmetric(vertical: 8),
                          padding: const EdgeInsets.all(12),
                          child: message['image'] != null
                              ? Image.network(
                                  message['image'],
                                  fit: BoxFit.cover,
                                )
                              : Text(
                                  message['content'],
                                  style: const TextStyle(
                                    color: Colors.white70,
                                    fontSize: 16,
                                  ),
                                ),
                        ),
                      );
                    },
                  ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: const BoxDecoration(
              color: Color(0xFF1E1E1E),
              border: Border(
                top: BorderSide(color: Color(0xFF64FFDA), width: 1.5),
              ),
            ),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.photo, color: Colors.white),
                  onPressed: _pickImage,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: _controller,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      hintText: 'Enter a message...',
                      hintStyle: const TextStyle(color: Colors.white38),
                      filled: true,
                      fillColor: const Color(0xFF2E2E2E),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 12,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                ElevatedButton(
                  onPressed: _sendMessage,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF64FFDA),
                    padding: const EdgeInsets.all(16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Icon(Icons.send, color: Colors.black),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
