import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiClient {
  Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('jwt_token');
  }

  Future<Map<String, String>> _getHeaders({Map<String, String>? extraHeaders}) async {
    final token = await _getToken();
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
      ...?extraHeaders,
    };
    return headers;
  }

  Future<http.Response> get(String endpoint, {Map<String, String>? headers}) async {
    final fullHeaders = await _getHeaders(extraHeaders: headers);
    final url = Uri.parse(endpoint);
    return http.get(url, headers: fullHeaders);
  }

  Future<http.Response> post(String endpoint, {Object? body, Map<String, String>? headers}) async {
    final fullHeaders = await _getHeaders(extraHeaders: headers);
    final url = Uri.parse(endpoint);
    return http.post(url, headers: fullHeaders, body: body);
  }

  Future<http.Response> put(String endpoint, {Object? body, Map<String, String>? headers}) async {
    final fullHeaders = await _getHeaders(extraHeaders: headers);
    final url = Uri.parse(endpoint);
    return http.put(url, headers: fullHeaders, body: jsonEncode(body));
  }

  Future<http.Response> delete(String endpoint, {Map<String, String>? headers}) async {
    final fullHeaders = await _getHeaders(extraHeaders: headers);
    final url = Uri.parse(endpoint);
    return http.delete(url, headers: fullHeaders);
  }

  Future<http.StreamedResponse> uploadMultipart({
    required String endpoint,
    required List<http.MultipartFile> files,
    Map<String, String>? fields,
  }) async {
    final token = await _getToken();
    final request = http.MultipartRequest('POST', Uri.parse(endpoint));

    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    if (fields != null) {
      request.fields.addAll(fields);
    }

    request.files.addAll(files);

    return request.send();
  }
}
