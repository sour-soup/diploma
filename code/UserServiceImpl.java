package org.soursoup.bimbim.service.impl;

import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.MinioConfig;
import org.soursoup.bimbim.dto.JwtDto;
import org.soursoup.bimbim.dto.request.ImageRequest;
import org.soursoup.bimbim.dto.request.UpdateImageRequest;
import org.soursoup.bimbim.dto.request.UserLoginRequest;
import org.soursoup.bimbim.dto.request.UserRegisterRequest;
import org.soursoup.bimbim.entity.Answer;
import org.soursoup.bimbim.entity.Question;
import org.soursoup.bimbim.entity.User;
import org.soursoup.bimbim.entity.UserCategory;
import org.soursoup.bimbim.exception.BadRequestException;
import org.soursoup.bimbim.exception.NotFoundException;
import org.soursoup.bimbim.exception.UnauthorizedException;
import org.soursoup.bimbim.repository.AnswerRepository;
import org.soursoup.bimbim.repository.QuestionRepository;
import org.soursoup.bimbim.repository.UserCategoryRepository;
import org.soursoup.bimbim.repository.UserRepository;
import org.soursoup.bimbim.service.ImageService;
import org.soursoup.bimbim.service.UserService;
import org.soursoup.bimbim.utils.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;
    private final JwtUtils jwtUtils;
    private final MinioConfig minioConfig;


    @Override
    public void registerUser(UserRegisterRequest userRegisterRequest) {
        if (userRepository.findByUsername(userRegisterRequest.username()).isPresent()) {
            throw new BadRequestException("Username already exists");
        }

        User newUser = new User();

        newUser.setUsername(userRegisterRequest.username());
        newUser.setPassword(passwordEncoder.encode(userRegisterRequest.password()));
        newUser.setGender(userRegisterRequest.gender());
        newUser.setDescription(userRegisterRequest.description());

        userRepository.save(newUser);
    }

    @Override
    public JwtDto loginUser(UserLoginRequest userLoginRequest) {
        Optional<User> user = userRepository.findByUsername(userLoginRequest.username());
        if (user.isEmpty()) {
            throw new UnauthorizedException("Username not found");
        }

        if (!passwordEncoder.matches(userLoginRequest.password(), user.get().getPassword())) {
            throw new UnauthorizedException("Wrong password");
        }

        return jwtUtils.generateToken(user.get().getUsername(), user.get().getId(), user.get().getRoles());
    }

    @Override
    public void updateAvatar(Long id, UpdateImageRequest updateAvatarReq) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        String filename = imageService.upload(new ImageRequest(updateAvatarReq.image()));
        user.get().setAvatar(filename);
        userRepository.save(user.get());
    }

    @Override
    public User getUser(Long id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        String avatar = user.get().getAvatar() == null ? "unknown.png" : user.get().getAvatar();

        user.get().setAvatar(minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + avatar);
        return user.get();
    }

    @Override
    @Transactional
    public void answerQuestion(Long userId, Long questionId, Long result) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question id doesn't exist"));
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Answer answer = answerRepository.findByUserAndQuestion(user, question)
                .orElse(new Answer());

        answer.setUser(user);
        answer.setQuestion(question);
        answer.setAnswer(result);

        UserCategory userCategory = userCategoryRepository.findByUserAndCategory(user, question.getCategory())
                .orElse(new UserCategory());

        userCategory.setUser(user);
        userCategory.setCategory(question.getCategory());
        userCategory.setNextQuestionPos(userCategory.getNextQuestionPos() + 1);

        userCategoryRepository.save(userCategory);
        answerRepository.save(answer);
        questionRepository.save(question);
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public void updateDescription(Long userId, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User doesn't exist"));
        user.setDescription(description);
        userRepository.save(user);
    }
}
