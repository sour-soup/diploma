package org.soursoup.bimbim.dto.response;

public record MessageResponse(Long id, Long chatId, Long authorId, boolean isMe, String content, String image) {
}
