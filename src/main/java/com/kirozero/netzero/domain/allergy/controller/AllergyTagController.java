package com.kirozero.netzero.domain.allergy.controller;

import com.kirozero.netzero.domain.allergy.dto.AllergyTagListResponse;
import com.kirozero.netzero.domain.allergy.service.AllergyTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/allergy-tags")
@RequiredArgsConstructor
public class AllergyTagController {

    private final AllergyTagService allergyTagService;

    @GetMapping
    public AllergyTagListResponse getAllergyTags() {
        return allergyTagService.getAllergyTags();
    }
}
