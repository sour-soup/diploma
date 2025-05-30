package org.soursoup.bimbim.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record ImageRequest(MultipartFile image) {
}

