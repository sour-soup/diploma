import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:bim_bim_app/constants/constants.dart';
import 'package:bim_bim_app/services/api_client.dart';

class PeoplePage extends StatefulWidget {
  const PeoplePage({super.key});

  @override
  State<PeoplePage> createState() => _PeoplePageState();
}

class _PeoplePageState extends State<PeoplePage> {
  final _apiClient = ApiClient();
  List<Map<String, String?>> _categories = [];
  List<Map<String, dynamic>> _people = [];
  String? _selectedGender;
  int? _selectedCategoryId = 1;
  bool _isDescending = true;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _fetchCategories();
    _fetchPeople(_selectedCategoryId); 
  }

  Future<void> _sendInvite(String toUserId) async {
    try {
      final response = await _apiClient.post('$baseUrl/chat/invite/$toUserId');

      if (response.statusCode == 200) {
        _showSuccessDialog('Request sent');
        _fetchPeople(_selectedCategoryId);
      } else {
        throw Exception('Failed to send invite: ${response.body}');
      }
    } catch (e) {
      _showErrorDialog('Error sending invite: $e');
    }
  }

  void _showSuccessDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Success!'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> _fetchCategories() async {
    try {
      setState(() {
        _isLoading = true;
      });

      final response = await _apiClient.get('$baseUrl/category/all');

      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List<dynamic>;
        setState(() {
          _categories = data.map((e) {
            final category = e as Map<String, dynamic>;
            return {
              'id': category['id']?.toString(),
              'name': category['name'] as String?,
            };
          }).toList();
          _isLoading = false;
        });
      } else {
        throw Exception('Failed to load categories');
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showErrorDialog('Error loading categories: $e');
    }
  }

  Future<void> _fetchPeople(int? categoryId) async {
    try {
      setState(() {
        _isLoading = true;
      });

      final response = await _apiClient.get('$baseUrl/matching/$categoryId');

      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List<dynamic>;
        setState(() {
          _people = data.map((e) => e as Map<String, dynamic>).toList();
          print(data);
          _isLoading = false;
        });
      } else {
        throw Exception('Failed to load people');
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showErrorDialog('Error loading people: $e');
    }
  }

  void _showErrorDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Error!'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final filteredPeople = _people.where((person) {
      if (_selectedGender == null) return true;
      return person['gender'] == _selectedGender;
    }).toList();

    final sortedPeople = List.of(filteredPeople);
    sortedPeople.sort((a, b) => _isDescending
        ? b['similarity'].compareTo(a['similarity'])
        : a['similarity'].compareTo(b['similarity']));

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    value: _selectedCategoryId?.toString(),
                    decoration: InputDecoration(
                      labelText: 'Category',
                      labelStyle: const TextStyle(color: Colors.white),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderSide: const BorderSide(color: Color(0xFFBB86FC)),
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    dropdownColor: const Color(0xFF1E1E1E),
                    style: const TextStyle(color: Colors.white),
                    items: _categories.map((category) {
                      return DropdownMenuItem(
                        value: category['id'],
                        child: Text(category['name'] ?? ''),
                      );
                    }).toList(),
                    onChanged: (value) {
                      setState(() {
                        _selectedCategoryId = value != null ? int.tryParse(value) : null;
                        _fetchPeople(_selectedCategoryId);
                      });
                    },
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    value: _selectedGender,
                    decoration: InputDecoration(
                      labelText: 'Gender',
                      labelStyle: const TextStyle(color: Colors.white),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderSide: const BorderSide(color: Color(0xFFBB86FC)),
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    dropdownColor: const Color(0xFF1E1E1E),
                    style: const TextStyle(color: Colors.white),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('All')),
                      DropdownMenuItem(value: 'male', child: Text('Men')),
                      DropdownMenuItem(value: 'female', child: Text('Women')),
                    ],
                    onChanged: (value) {
                      setState(() {
                        _selectedGender = value;
                      });
                    },
                  ),
                ),
                const SizedBox(width: 10),
                IconButton(
                  icon: Icon(
                    _isDescending ? Icons.arrow_downward : Icons.arrow_upward,
                    color: const Color(0xFFBB86FC),
                  ),
                  onPressed: () {
                    setState(() {
                      _isDescending = !_isDescending;
                    });
                  },
                ),
              ],
            ),
            const SizedBox(height: 20),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(
                  color: Color(0xFFBB86FC),
                ),
              )
            else if (sortedPeople.isEmpty)
              const Center(
                child: Text(
                  'No people in this category',
                  style: TextStyle(color: Colors.white70),
                ),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: sortedPeople.length,
                  itemBuilder: (context, index) {
                    final person = sortedPeople[index];
                    return Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFF1E1E1E),
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFBB86FC).withOpacity(0.2),
                            blurRadius: 10,
                            offset: const Offset(2, 4),
                          ),
                        ],
                      ),
                      margin: const EdgeInsets.symmetric(vertical: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: const Color(0xFFBB86FC),
                          backgroundImage: NetworkImage(person['avatar']),
                        ),
                        title: Text(
                          person['username'],
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        subtitle: Text(
                          'Similarity: ${person['similarity']}%',
                          style: const TextStyle(
                            color: Colors.white70,
                          ),
                        ),
                        trailing: IconButton(
                          icon: Stack(
                            children: [
                              const Icon(
                                Icons.mail,
                                size: 28,
                                color: Color(0xFFBB86FC),
                              ),
                              Positioned(
                                right: -2,
                                bottom: -2,
                                child: Container(
                                  width: 12,
                                  height: 12,
                                  decoration: const BoxDecoration(
                                    color: Colors.black,
                                    shape: BoxShape.circle,
                                  ),
                                  child: const Icon(
                                    Icons.add,
                                    size: 12,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          onPressed: () {
                            _sendInvite(person['id'].toString());
                          },
                        ),
                        onTap: () {
                          _showPersonDetails(context, person);
                        },
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }

  void _showPersonDetails(BuildContext context, Map<String, dynamic> person) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          backgroundColor: const Color(0xFF1E1E1E),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Align(
                  alignment: Alignment.topRight,
                  child: IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
                CircleAvatar(
                  radius: 60,
                  backgroundColor: const Color(0xFFBB86FC),
                  backgroundImage: NetworkImage(person['avatar']),
                ),
                const SizedBox(height: 20),
                Text(
                  person['username'],
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  'Similarity: ${person['similarity']}%',
                  style: const TextStyle(
                    fontSize: 18,
                    color: Colors.white70,
                  ),
                ),
                const SizedBox(height: 10),
                if (person['description'] != null && person['description'].toString().trim().isNotEmpty)
                  Text(
                    person['description'],
                    style: const TextStyle(
                      fontSize: 16,
                      color: Colors.white70,
                    ),
                    textAlign: TextAlign.center,
                  ),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: () {
                    _sendInvite(person['id'].toString());
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFBB86FC),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    'Invite to chat',
                    style: TextStyle(
                      color: Colors.black,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
