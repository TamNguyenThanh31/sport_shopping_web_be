package com.runner.shopping.service;

import com.runner.shopping.model.dto.PromotionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    PromotionDTO createPromotion(Long staffId, PromotionDTO promotionDTO);

    PromotionDTO updatePromotion(Long staffId, Long promotionId, PromotionDTO promotionDTO);

    void deletePromotion(Long staffId, Long promotionId);

    PromotionDTO getPromotionById(Long promotionId);

    List<PromotionDTO> getAllPromotions();

    Page<PromotionDTO> getPromotions(String code, Boolean isActive,
                                     LocalDateTime dateFrom, LocalDateTime dateTo,
                                     Pageable pageable);
}