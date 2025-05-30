package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.security.JwtUserDetails;
import org.soursoup.bimbim.dto.JwtDto;
import org.soursoup.bimbim.dto.request.UserLoginRequest;
import org.soursoup.bimbim.dto.request.UserRegisterRequest;
import org.soursoup.bimbim.service.impl.UserServiceImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserServiceImpl userService;

    @GetMapping("/isAdmin")
    @SecurityRequirement(name = "bearerAuth")
    public boolean isAdmin(@AuthenticationPrincipal JwtUserDetails userDetails) {
        return "ADMIN".equals(userDetails.getRoles());
    }

    @PostMapping("/register")
    public void registerUser(@RequestBody UserRegisterRequest userRegisterRequest) {
        userService.registerUser(userRegisterRequest);
    }

    @PostMapping("/login")
    public JwtDto loginUser(@RequestBody UserLoginRequest userLoginRequest) {
        return userService.loginUser(userLoginRequest);
    }
}
