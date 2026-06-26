package com.kirozero.netzero.domain.upload.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadsPhotoThroughBackendAndReturnsStoredUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cooked.jpg",
                "image/jpeg",
                "demo-image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/uploads")
                        .file(file)
                        .param("purpose", "COOKED_PHOTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl", containsString("/uploads/cooked-photo/")))
                .andExpect(jsonPath("$.objectKey", containsString("uploads/cooked-photo/")))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.size").value(10));
    }
}
