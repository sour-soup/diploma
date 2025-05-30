import 'package:flutter/material.dart';
import 'package:bim_bim_app/constants/constants.dart';
import 'package:bim_bim_app/screens/pages/admin_page.dart';
import 'package:bim_bim_app/services/api_client.dart';

import '../../screens/pages/people_page.dart';
import '../../screens/pages/tests_page.dart';
import '../../screens/pages/messenger_page.dart';
import '../../screens/pages/profile_page.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  final _apiClient = ApiClient();
  int _currentIndex = 0;
  bool _isAdmin = false;

  final List<Widget> _pages = [
    const QuestionsPage(), 
    const PeoplePage(),
    const MessengerPage(),
    const AdminPage(),
  ];

  final List<String> _titles = ['Tests', 'Recommendations', 'Chats', 'Admin Panel'];

  @override
  void initState() {
    super.initState();
    _checkAdminStatus();
  }

  Future<void> _checkAdminStatus() async {
    try {
      final response = await _apiClient.get('$baseUrl/auth/isAdmin');

      if (response.statusCode == 200) {
        final data = response.body;
        setState(() {
          _isAdmin = data == "true";
        });
      } else {
        print('Failed to check admin status: ${response.body}');
      }
    } catch (e) {
      print('Error checking admin status: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _titles[_currentIndex],
          style: const TextStyle(
            color: Colors.white,
            shadows: [
              Shadow(
                color: Colors.green,
                blurRadius: 5,
              ),
            ],
          ),
        ),
        backgroundColor: Colors.black,
        actions: [
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const ProfilePage()),
              );
            },
          ),
        ],
      ),
      body: _pages[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        items: [
          const BottomNavigationBarItem(
            icon: Icon(Icons.library_books),
            label: 'Tests',
          ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.people),
            label: 'People',
          ),
          const BottomNavigationBarItem(
            icon: Icon(Icons.message),
            label: 'Messenger',
          ),
          if (_isAdmin)
            const BottomNavigationBarItem(
              icon: Icon(Icons.admin_panel_settings),
              label: 'Admin',
            ),
        ],
      ),
    );
  }
}
