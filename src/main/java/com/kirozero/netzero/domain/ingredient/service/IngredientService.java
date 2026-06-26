package com.kirozero.netzero.domain.ingredient.service;

import com.kirozero.netzero.domain.ingredient.dto.IngredientItemResponse;
import com.kirozero.netzero.domain.ingredient.dto.IngredientSearchResponse;
import com.kirozero.netzero.domain.ingredient.entity.IngredientMaster;
import com.kirozero.netzero.domain.ingredient.repository.IngredientMasterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientMasterRepository ingredientMasterRepository;

    @Transactional(readOnly = true)
    public IngredientSearchResponse search(String keyword) {
        String normalizedKeyword = normalize(keyword);
        List<IngredientMaster> ingredients = normalizedKeyword == null
                ? ingredientMasterRepository.findTop20ByOrderByIdAsc()
                : ingredientMasterRepository.findTop20ByNameKoContainingOrderByNameKoAsc(normalizedKeyword);

        return new IngredientSearchResponse(
                ingredients.stream()
                        .map(IngredientItemResponse::from)
                        .toList()
        );
    }

    private String normalize(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
