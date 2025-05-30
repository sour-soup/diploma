package org.soursoup.bimbim.service.impl;

import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.dto.request.ImageRequest;
import org.soursoup.bimbim.dto.request.UpdateImageRequest;
import org.soursoup.bimbim.entity.Chat;
import org.soursoup.bimbim.entity.Message;
import org.soursoup.bimbim.entity.User;
import org.soursoup.bimbim.exception.ForbiddenException;
import org.soursoup.bimbim.exception.NotFoundException;
import org.soursoup.bimbim.repository.ChatRepository;
import org.soursoup.bimbim.repository.MessageRepository;
import org.soursoup.bimbim.repository.UserRepository;
import org.soursoup.bimbim.service.ChatService;
import org.soursoup.bimbim.service.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    public List<Chat> getSentRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return chatRepository.findAllByFromUserAndToUserConfirmedFalse(user)
                .stream().filter(chat -> !chat.isCanceled()).toList();

    }

    public List<Chat> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return chatRepository.findAllByToUserAndToUserConfirmedFalse(user)
                .stream().filter(chat -> !chat.isCanceled()).toList();
    }

    @Transactional
    public void acceptChatRequest(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat request not found"));

        if (!chat.getToUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied: You are not allowed to accept this chat request");
        }

        chat.setToUserConfirmed(true);
        chatRepository.save(chat);
    }

    @Transactional
    public void declineChatRequest(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat request not found"));

        if (!chat.getToUser().getId().equals(userId) && !chat.getFromUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied: You are not allowed to accept this chat request");
        }
        chat.setCanceled(true);
        chatRepository.save(chat);
    }

    @Override
    @Transactional
    public Message uploadImage(Long userId, Long chatId, UpdateImageRequest updateImageRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        if (!(chat.getFromUser().getId().equals(userId) || chat.getToUser().getId().equals(userId))) {
            throw new ForbiddenException("User is not part of the chat");
        }

        if (!chat.isToUserConfirmed()) {
            throw new ForbiddenException("Chat is not confirmed");
        }

        String imageFilename = imageService.upload(new ImageRequest(updateImageRequest.image()));

        Message message = new Message();
        message.setChat(chat);
        message.setAuthor(user);
        message.setContent("");
        message.setImage(imageFilename);

        return messageRepository.save(message);
    }


    public List<Chat> getActiveChats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return chatRepository.findAllByFromUserAndToUserConfirmedTrueOrToUserAndToUserConfirmedTrue(user, user)
                .stream().filter(chat -> !chat.isCanceled()).toList();
    }

    @Transactional
    public Message sendMessage(Long chatId, Long authorId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        if (!chat.isToUserConfirmed()) {
            throw new ForbiddenException("Chat is not confirmed");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setAuthor(author);
        message.setContent(content);
        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        return messageRepository.findAllByChatOrderBySentAt(chat);
    }

    @Transactional
    public Chat createChatRequest(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new ForbiddenException("You cannot invite yourself to a chat");
        }

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new NotFoundException("Sender not found"));

        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));

        boolean chatExists = chatRepository.existsByFromUserAndToUser(fromUser, toUser) ||
                             chatRepository.existsByFromUserAndToUser(toUser, fromUser);

        if (chatExists) {
            throw new ForbiddenException("Chat request already exists");
        }

        Chat chat = new Chat();
        chat.setFromUser(fromUser);
        chat.setToUser(toUser);
        chat.setToUserConfirmed(false);

        return chatRepository.save(chat);
    }


}
