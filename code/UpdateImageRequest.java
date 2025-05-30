package org.soursoup.bimbim.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record UpdateImageRequest(MultipartFile image) {
}
