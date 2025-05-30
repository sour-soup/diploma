package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.MinioConfig;
import org.soursoup.bimbim.config.security.JwtUserDetails;
import org.soursoup.bimbim.dto.request.UpdateImageRequest;
import org.soursoup.bimbim.dto.response.ChatResponse;
import org.soursoup.bimbim.dto.response.MessageResponse;
import org.soursoup.bimbim.entity.Chat;
import org.soursoup.bimbim.entity.Message;
import org.soursoup.bimbim.entity.User;
import org.soursoup.bimbim.repository.UserRepository;
import org.soursoup.bimbim.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/chat")
public class ChatController {

    private final ChatService charService;
    private final UserRepository userRepository;
    private final MinioConfig minioConfig;

    @GetMapping("/sent-requests")
    @SecurityRequirement(name = "bearerAuth")
    public List<ChatResponse> getSentRequests(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return charService.getSentRequests(userId).stream().map(chat -> toDto(chat, userId)).toList();
    }

    @GetMapping("/pending-requests")
    @SecurityRequirement(name = "bearerAuth")
    public List<ChatResponse> getPendingRequests(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return charService.getPendingRequests(userId).stream().map(chat -> toDto(chat, userId)).toList();
    }

    @GetMapping("/active")
    @SecurityRequirement(name = "bearerAuth")
    public List<ChatResponse> getActiveChats(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return charService.getActiveChats(userId).stream().map(chat -> toDto(chat, userId)).toList();
    }

    @PostMapping("/{chatId}/messages")
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse sendMessage(@PathVariable Long chatId, @RequestBody String content,
                                       @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return toDto(charService.sendMessage(chatId, userId, content), userId);
    }

    @PostMapping(value = "/{chatId}/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse uploadImage(@PathVariable Long chatId, @ModelAttribute UpdateImageRequest updateImageRequest,
                                       @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return toDto(charService.uploadImage(userId, chatId, updateImageRequest), userId);
    }

    @GetMapping("/{chatId}/messages")
    @SecurityRequirement(name = "bearerAuth")
    public List<MessageResponse> getMessages(@PathVariable Long chatId,
                                             @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return charService.getMessages(chatId).stream().map(message -> toDto(message, userId)).toList();
    }

    @PostMapping("/invite/{toUserId}")
    @SecurityRequirement(name = "bearerAuth")
    public ChatResponse inviteToChat(@PathVariable Long toUserId,
                                     @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long fromUserId = userDetails.getId();
        return toDto(charService.createChatRequest(fromUserId, toUserId), fromUserId);
    }

    @PostMapping("/{chatId}/accept")
    @SecurityRequirement(name = "bearerAuth")
    public void acceptChatRequest(@PathVariable Long chatId,
                                  @AuthenticationPrincipal JwtUserDetails userDetails) {
        charService.acceptChatRequest(chatId, userDetails.getId());
    }

    @PostMapping("/{chatId}/decline")
    @SecurityRequirement(name = "bearerAuth")
    public void declineChatRequest(@PathVariable Long chatId,
                                   @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        charService.declineChatRequest(chatId, userId);
    }

    private ChatResponse toDto(Chat chat, Long userId) {
        Long toUserId = chat.getFromUser().getId() + chat.getToUser().getId() - userId;
        User user = userRepository.findById(toUserId).orElseThrow();

        return new ChatResponse(
                chat.getId(),
                user.getId(),
                user.getUsername()
        );
    }

    private MessageResponse toDto(Message message, Long userId) {
        if (message.getImage() != null) {
            message.setImage(minioConfig.getUrl() + "/" + minioConfig.getBucket() + "/" + message.getImage());
        }
        return new MessageResponse(
                message.getId(),
                message.getChat().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getId().equals(userId),
                message.getContent(),
                message.getImage()
        );
    }
}

