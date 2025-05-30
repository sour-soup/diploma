package org.soursoup.bimbim.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.soursoup.bimbim.config.security.JwtUserDetails;
import org.soursoup.bimbim.dto.response.MatchingResponse;
import org.soursoup.bimbim.service.MatchingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matching")
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/{categoryId}")
    @SecurityRequirement(name = "bearerAuth")
    public List<MatchingResponse> getMatching(@PathVariable Long categoryId,
                                              @AuthenticationPrincipal JwtUserDetails userDetails) {
        return matchingService.getMatching(userDetails.getId(), categoryId);
    }
}
