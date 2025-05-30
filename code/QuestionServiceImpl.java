package org.soursoup.bimbim.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.MinioConfig;
import org.soursoup.bimbim.entity.Category;
import org.soursoup.bimbim.entity.Question;
import org.soursoup.bimbim.repository.CategoryRepository;
import org.soursoup.bimbim.repository.QuestionRepository;
import org.soursoup.bimbim.service.QuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final MinioConfig minioConfig;

    public Question addQuestion(String questionContent, String answerLeft, String answerRight, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + categoryId + " not found"));


        Question question = new Question();
        question.setContent(questionContent);
        question.setAnswerLeft(answerLeft);
        question.setAnswerRight(answerRight);
        question.setCategory(category);

        category.setQuestionCount(category.getQuestionCount() + 1);
        categoryRepository.save(category);

        return questionRepository.save(question);
    }

    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question with ID " + questionId + " not found"));
        Category category = question.getCategory();
        category.setQuestionCount(category.getQuestionCount() - 1);
        categoryRepository.save(category);
        questionRepository.deleteById(questionId);
    }

    @Override
    public List<Question> getQuestions() {
        return questionRepository.findAll().stream()
                .peek(question -> {
                    if (question.getImage() != null) {
                        question.setImage(minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + question.getImage());
                    }
                }).toList();
    }

    @Override
    public List<Question> getQuestionsByCategory(Long categoryId) {
        return questionRepository.findAllByCategoryId(categoryId).stream()
                .peek(question -> {
                    if (question.getImage() != null) {
                        question.setImage(minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + question.getImage());
                    }
                }).toList();
    }

    @Override
    public List<Question> getRemainderQuestions(Long userId, Long categoryId) {
        return questionRepository.findAllByCategoryId(categoryId).stream()
                .filter(question -> question.getAnswers().stream()
                        .noneMatch(answer -> answer.getUser().getId().equals(userId)))
                .peek(question -> {
                    if (question.getImage() != null) {
                        question.setImage(minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + question.getImage());
                    }
                })
                .toList();
    }
}
