package org.soursoup.bimbim.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(Long id, String username, String gender, String description, String avatar) {
}
