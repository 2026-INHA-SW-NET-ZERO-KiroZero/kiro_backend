package com.kirozero.netzero.domain.ingredient.controller;

import com.kirozero.netzero.domain.ingredient.dto.IngredientSearchResponse;
import com.kirozero.netzero.domain.ingredient.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public IngredientSearchResponse searchIngredients(@RequestParam(required = false) String keyword) {
        return ingredientService.search(keyword);
    }
}
