package org.soursoup.bimbim.dto.request;

public record AddQuestionRequest(String questionContent, String answerLeft, String answerRight, Long categoryId) {
}
