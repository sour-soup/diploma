package org.soursoup.bimbim.dto.response;

public record MatchingResponse(
        Long id,
        String avatar,
        String gender,
        String username,
        String description,
        Long similarity) {
}
