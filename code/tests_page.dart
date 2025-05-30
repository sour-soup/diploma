import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:bim_bim_app/services/api_client.dart';
import 'package:swipe_cards/swipe_cards.dart';
import '../../constants/constants.dart';

String decodeUtf8(String input) {
  return utf8.decode(Uint8List.fromList(input.codeUnits));
}

class QuestionItem {
  final String id;
  final String content;
  final String answerLeft;
  final String answerRight;

  QuestionItem({required this.id, required this.content, required this.answerLeft, required this.answerRight});
}

class QuestionsPage extends StatefulWidget {
  const QuestionsPage({super.key});

  @override
  State<QuestionsPage> createState() => _QuestionsPageState();
}

class _QuestionsPageState extends State<QuestionsPage> {
  final _apiClient = ApiClient();
  String _selectedCategory = '';
  List<String> _categories = [];
  List<Map<String, dynamic>> _questions = [];
  Map<String, String> _categoryMap = {};
  late List<SwipeItem> _swipeItems = [];
  late MatchEngine _matchEngine;

  @override
  void initState() {
    super.initState();
    _loadCategories();
  }

  Future<void> _loadCategories() async {
    try {
      final response = await _apiClient.get('$baseUrl/category/all');
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List;
        setState(() {
          _categories = data.map((category) {
            var name = category['name'];
            return name is String
                ? name
                : name.toString();
          }).toList();

          _categoryMap = {
            for (var item in data)
              item['name'] is String ? item['name'] : item['name'].toString():
                  (item['id'] is String
                      ? item['id']
                      : item['id'].toString())
          };

          if (_categories.isNotEmpty) {
            _selectedCategory = _categories[0];
            _loadAllQuestions(_selectedCategory);
          }
        });
      } else {
        throw Exception('Failed to load categories');
      }
    } catch (e) {
      print('Error loading categories: $e');
    }
  }

  Future<void> _loadAllQuestions(String categoryName) async {
    final categoryId = _categoryMap[categoryName];
    final intId = int.tryParse(categoryId ?? '0');

    try {
      final response = await _apiClient.get('$baseUrl/question/remainderByCategory?categoryId=$intId');

      print(response.body);
      print(response.statusCode);

      if (response.statusCode == 200) {
        final decodedBody = decodeUtf8(response.body);
        final data = json.decode(decodedBody) as List;

        if (data.isNotEmpty) {
          setState(() {
            _questions = data
                .map((q) => {'id': q['id'], 'content': q['content'], 'answerLeft' : q['answerLeft'], 'answerRight' : q['answerRight']})
                .toList();
            _initializeSwipeItems();
          });
        } else {
          print("No questions found for this category");
        }
      }
    } catch (e) {
      print('Error loading questions: $e');
    }
  }

  void _initializeSwipeItems() {
    if (_questions.isNotEmpty) {
      _swipeItems = _questions.map((question) {
        QuestionItem questionItem = QuestionItem(
          id: question['id'].toString(),
          content: question['content'],
          answerLeft: question['answerLeft'],
          answerRight: question['answerRight'],
        );

        return SwipeItem(
          content: questionItem,
          likeAction: () => _onAnswer(questionItem.id, 1),
          nopeAction: () => _onAnswer(questionItem.id, -1),
          superlikeAction: () => _onAnswer(questionItem.id, 0),
        );
      }).toList();

      setState(() {
        _matchEngine = MatchEngine(swipeItems: _swipeItems);
      });
    }
  }

  Future<void> _sendAnswer(String questionId, int value) async {
    try {
      final response = await _apiClient.get('$baseUrl/question/setAnswer?questionId=$questionId&result=$value');

      if (response.statusCode == 200) {
        print('Answer saved successfully for question $questionId');
      } else {
        print('Failed to save answer: ${response.body}');
      }
    } catch (e) {
      print('Error saving answer: $e');
    }
  }

  void _onAnswer(String questionId, int answer) async {
    if (_swipeItems.isNotEmpty) {
      await _sendAnswer(questionId, answer);

      setState(() {
        if (_swipeItems.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('No more questions)')),
          );
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;

    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            DropdownButtonFormField<String>(
              value: _selectedCategory.isEmpty ? null : _selectedCategory,
              decoration: InputDecoration(
                labelText: 'Category',
                labelStyle: const TextStyle(color: Colors.white),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                enabledBorder: OutlineInputBorder(
                  borderSide: const BorderSide(color: Color(0xFF64FFDA)),
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              dropdownColor: const Color(0xFF1E1E1E),
              style: const TextStyle(color: Colors.white),
              items: _categories
                  .map((category) => DropdownMenuItem(
                        value: category,
                        child: Text(category),
                      ))
                  .toList(),
              onChanged: (value) async {
                if (value != null) {
                  setState(() {
                    _selectedCategory = value;
                    _questions.clear();
                    _loadAllQuestions(value);
                  });
                }
              },
            ),
            const SizedBox(height: 20),
            Expanded(
              child: _questions.isEmpty
                  ? const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.done_all,
                            color: Colors.green,
                            size: 64,
                          ),
                          SizedBox(height: 20),
                          Text(
                            'No more questions!',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    )
                  : SwipeCards(
                      matchEngine: _matchEngine,
                      itemBuilder: (context, index) {
                        final question = _swipeItems[index].content.content as String;

                        final leftOption =
                            _swipeItems[index].content.answerLeft as String;

                        final rightOption =
                            _swipeItems[index].content.answerRight as String;

                        return Container(
                          width: screenWidth * 0.9,
                          height: screenHeight * 0.7,
                          decoration: BoxDecoration(
                            color: Colors.black,
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.green.withOpacity(0.5),
                                blurRadius: 20,
                                offset: const Offset(-5, 5),
                              ),
                              BoxShadow(
                                color: Colors.purple.withOpacity(0.5),
                                blurRadius: 20,
                                offset: const Offset(5, 5),
                              ),
                            ],
                          ),
                          child: Stack(
                            children: [
                              Positioned(
                                top: screenHeight * 0.05,
                                left: 20,
                                right: 20,
                                child: Text(
                                  question,
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    fontSize: question.length > 20 ? 28 : 36,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                    shadows: const [
                                      Shadow(
                                        color: Colors.white,
                                        blurRadius: 10,
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              Positioned(
                                left: 20,
                                top: screenHeight * 0.35,
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.arrow_back,
                                      color: Color(0xFF64FFDA),
                                      size: 28,
                                    ),
                                    const SizedBox(width: 8),
                                    Text(
                                      leftOption,
                                      style: const TextStyle(
                                        fontSize: 28,
                                        fontWeight: FontWeight.bold,
                                        color: Color(0xFF64FFDA),
                                        shadows: [
                                          Shadow(
                                            color: Color(0xFF64FFDA),
                                            blurRadius: 10,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Positioned(
                                right: 20,
                                top: screenHeight * 0.35,
                                child: Row(
                                  children: [
                                    Text(
                                      rightOption,
                                      style: const TextStyle(
                                        fontSize: 28,
                                        fontWeight: FontWeight.bold,
                                        color: Color(0xFFBB86FC),
                                        shadows: [
                                          Shadow(
                                            color: Color(0xFFBB86FC),
                                            blurRadius: 10,
                                          ),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    const Icon(
                                      Icons.arrow_forward,
                                      color: Color(0xFFBB86FC),
                                      size: 28,
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                      onStackFinished: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('No more questions)')),
                        );
                        setState(() {
                          _questions.clear();
                        });
                      },
                      upSwipeAllowed: false,
                      fillSpace: true,
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
