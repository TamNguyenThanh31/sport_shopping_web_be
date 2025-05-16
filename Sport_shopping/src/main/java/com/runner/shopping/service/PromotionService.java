package com.runner.shopping.service;

import com.runner.shopping.model.dto.PromotionDTO;

import java.util.List;

public interface PromotionService {

    PromotionDTO createPromotion(Long staffId, PromotionDTO promotionDTO);

    PromotionDTO updatePromotion(Long staffId, Long promotionId, PromotionDTO promotionDTO);

    void deletePromotion(Long staffId, Long promotionId);

    PromotionDTO getPromotionById(Long promotionId);

    List<PromotionDTO> getAllPromotions();
}
