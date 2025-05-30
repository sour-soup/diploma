package org.soursoup.bimbim.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record MatchingRequest(
        @JsonProperty Long userId,
        @JsonProperty List<QuestionMatchingRequest> questions,
        @JsonProperty List<UserMatchingRequest> users) {

    public record QuestionMatchingRequest(
            @JsonProperty Long id,
            @JsonProperty String content,
            @JsonProperty String answerLeft,
            @JsonProperty String answerRight) {
    }

    public record UserMatchingRequest(
            @JsonProperty Long id,
            @JsonProperty String avatar,
            @JsonProperty String gender,
            @JsonProperty String username,
            @JsonProperty String description,
            @JsonProperty Map<Long, Long> answers) {
    }
}
