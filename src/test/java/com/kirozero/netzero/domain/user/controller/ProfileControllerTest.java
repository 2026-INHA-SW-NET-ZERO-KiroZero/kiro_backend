package com.kirozero.netzero.domain.user.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void updatesMyProfileWithoutChangingEmail() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "profile-update@inha.edu",
                                  "password": "password1234",
                                  "nickname": "처음닉네임",
                                  "cookingSkill": "LOW",
                                  "allergyTags": ["egg"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = signupResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(put("/api/v1/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "양배추마스터",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": ["soy", "soy", "milk"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-update@inha.edu"))
                .andExpect(jsonPath("$.nickname").value("양배추마스터"))
                .andExpect(jsonPath("$.cookingSkill").value("MEDIUM"))
                .andExpect(jsonPath("$.cash").value(0))
                .andExpect(jsonPath("$.allergyTags", contains("soy", "milk")));
    }
}
