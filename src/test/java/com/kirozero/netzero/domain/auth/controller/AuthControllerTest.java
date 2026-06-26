package com.kirozero.netzero.domain.auth.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signsUpInhaUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth-signup@inha.edu",
                                  "password": "password1234",
                                  "nickname": "태훈",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": ["crustacean_shellfish"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("auth-signup@inha.edu"))
                .andExpect(jsonPath("$.nickname").value("태훈"))
                .andExpect(jsonPath("$.cookingSkill").value("MEDIUM"))
                .andExpect(jsonPath("$.cash").value(0))
                .andExpect(jsonPath("$.allergyTags", contains("crustacean_shellfish")))
                .andExpect(jsonPath("$.token", not(emptyString())));
    }

    @Test
    void rejectsNonInhaEmailForSignup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "outsider@example.com",
                                  "password": "password1234",
                                  "nickname": "외부인",
                                  "cookingSkill": "LOW",
                                  "allergyTags": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsInAndReadsCurrentUserWithBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth-login@inha.ac.kr",
                                  "password": "password1234",
                                  "nickname": "양파볶는중",
                                  "cookingSkill": "HIGH",
                                  "allergyTags": ["egg", "milk"]
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "auth-login@inha.ac.kr",
                                  "password": "password1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.nickname").value("양파볶는중"))
                .andExpect(jsonPath("$.cash").value(0))
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andReturn();

        String token = loginResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.email").value("auth-login@inha.ac.kr"))
                .andExpect(jsonPath("$.nickname").value("양파볶는중"))
                .andExpect(jsonPath("$.cookingSkill").value("HIGH"))
                .andExpect(jsonPath("$.cash").value(0))
                .andExpect(jsonPath("$.allergyTags", contains("egg", "milk")));
    }
}
