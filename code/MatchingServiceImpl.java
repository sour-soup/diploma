package org.soursoup.bimbim.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.soursoup.bimbim.client.MatchingClient;
import org.soursoup.bimbim.config.MinioConfig;
import org.soursoup.bimbim.dto.request.MatchingRequest;
import org.soursoup.bimbim.dto.response.MatchingResponse;
import org.soursoup.bimbim.entity.Answer;
import org.soursoup.bimbim.entity.Category;
import org.soursoup.bimbim.entity.Question;
import org.soursoup.bimbim.entity.User;
import org.soursoup.bimbim.exception.NotFoundException;
import org.soursoup.bimbim.repository.*;
import org.soursoup.bimbim.service.MatchingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MatchingClient matchingClient;
    private final MinioConfig minioConfig;

    @Override
    public List<MatchingResponse> getMatching(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        List<Question> questions = questionRepository.findAllByCategory(category);

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<User> connectedUserIds = chatRepository.findAllByFromUserOrToUser(currentUser, currentUser).stream()
                .flatMap(chat -> Stream.of(chat.getFromUser(), chat.getToUser()))
                .filter(user -> !user.equals(currentUser))
                .distinct().toList();

        List<User> users = userRepository.findAll().stream()
                .filter(user -> !connectedUserIds.contains(user))
                .toList();

        Map<Long, Map<Long, Long>> answerMap = buildAnswerMap(users, questions);

        List<MatchingRequest.UserMatchingRequest> userMatchingRequests = users.stream()
                .map(user -> buildUserMatching(user, answerMap))
                .toList();

        List<MatchingRequest.QuestionMatchingRequest> questionMatchingRequests = questions.stream()
                .map(this::buildQuestionMatching)
                .toList();

        MatchingRequest matchingRequest = new MatchingRequest(
                userId,
                questionMatchingRequests,
                userMatchingRequests
        );
        log.info("Matching request: {}", matchingRequest);

        var response = matchingClient.getMatching(matchingRequest);

        log.info("Matching response: {}", response);
        return response;
    }

    private Map<Long, Map<Long, Long>> buildAnswerMap(List<User> users, List<Question> questions) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, user -> buildAnswers(user, questions)));
    }

    private Map<Long, Long> buildAnswers(User user, List<Question> questions) {
        List<Answer> answers = answerRepository.findAllByUser(user);

        return answers.stream()
                .filter(answer -> questions.stream().anyMatch(q -> q.getId().equals(answer.getQuestion().getId())))
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Answer::getAnswer));
    }

    private MatchingRequest.UserMatchingRequest buildUserMatching(User user, Map<Long, Map<Long, Long>> answerMap) {
        return new MatchingRequest.UserMatchingRequest(
                user.getId(),
                defineAvatar(user.getAvatar()),
                user.getGender(),
                user.getUsername(),
                user.getDescription(),
                answerMap.get(user.getId())
        );
    }

    private MatchingRequest.QuestionMatchingRequest buildQuestionMatching(Question question) {
        return new MatchingRequest.QuestionMatchingRequest(
                question.getId(),
                question.getContent(),
                question.getAnswerLeft(),
                question.getAnswerRight()
        );
    }

    private String defineAvatar(String avatar) {
        avatar = Optional.ofNullable(avatar).orElse("unknown.png");

        return minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + avatar;
    }
}
