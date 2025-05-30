package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.security.JwtUserDetails;
import org.soursoup.bimbim.dto.request.UpdateImageRequest;
import org.soursoup.bimbim.dto.response.UserResponse;
import org.soursoup.bimbim.mapper.UserMapper;
import org.soursoup.bimbim.service.impl.UserServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserServiceImpl userService;
    private final UserMapper userMapper;

    @PostMapping(value = "/updateAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public void updateAvatar(@ModelAttribute UpdateImageRequest updateImageRequest,
                             @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        userService.updateAvatar(userId, updateImageRequest);
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse getUserProfile(@AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        return userMapper.toDto(userService.getUser(userId));
    }

    @GetMapping("/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse getUser(@PathVariable Long userId) {
        return userMapper.toDto(userService.getUser(userId));
    }

    @GetMapping("/all")
    @SecurityRequirement(name = "bearerAuth")
    public List<UserResponse> all() {
        return userService.getUsers().stream().map(userMapper::toDto).toList();
    }

    @PostMapping("/setDescription")
    @SecurityRequirement(name = "bearerAuth")
    public void setDescription(@RequestBody String description,
                               @AuthenticationPrincipal JwtUserDetails userDetails) {
        Long userId = userDetails.getId();
        userService.updateDescription(userId, description);
    }
}

