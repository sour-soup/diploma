package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.security.JwtUserDetails;
import org.soursoup.bimbim.dto.request.AddQuestionRequest;
import org.soursoup.bimbim.dto.response.QuestionResponse;
import org.soursoup.bimbim.mapper.QuestionMapper;
import org.soursoup.bimbim.service.QuestionService;
import org.soursoup.bimbim.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question")
public class QuestionController {

    private final UserService userService;
    private final QuestionService questionService;

    private final QuestionMapper questionMapper;

    @GetMapping("/setAnswer")
    @SecurityRequirement(name = "bearerAuth")
    public void setQuestionForUser(Long questionId, Long result, @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        userService.answerQuestion(userId, questionId, result);
    }


    @PostMapping(value = "/add")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public QuestionResponse addQuestion(@RequestBody AddQuestionRequest addQuestionRequest) {
        return questionMapper.toDto(questionService.addQuestion(
                addQuestionRequest.questionContent(),
                addQuestionRequest.answerLeft(),
                addQuestionRequest.answerRight(),
                addQuestionRequest.categoryId()
        ));
    }

    @DeleteMapping("/delete/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }

    @GetMapping("/allByCategory")
    @SecurityRequirement(name = "bearerAuth")
    public List<QuestionResponse> all(Long categoryId) {
        return questionService.getQuestionsByCategory(categoryId).stream().map(questionMapper::toDto).toList();
    }

    @GetMapping("/remainderByCategory")
    @SecurityRequirement(name = "bearerAuth")
    public List<QuestionResponse> remainderByCategory(Long categoryId,
                                                      @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return questionService.getRemainderQuestions(userId, categoryId).stream().map(questionMapper::toDto).toList();
    }

    @GetMapping("/all")
    @SecurityRequirement(name = "bearerAuth")
    public List<QuestionResponse> all() {
        return questionService.getQuestions().stream().map(questionMapper::toDto).toList();
    }

}
