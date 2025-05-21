package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Promotions;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.exception.ResourceNotFoundException;
import com.runner.shopping.mapper.PromotionMapper;
import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.repository.PromotionRepository;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final PromotionMapper promotionMapper;

    @Override
    @Transactional
    public PromotionDTO createPromotion(Long staffId, PromotionDTO promotionDTO) {
        validateStaff(staffId);
        validatePromotionDates(promotionDTO);
        if (promotionRepository.existsByCode(promotionDTO.getCode())) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại: " + promotionDTO.getCode());
        }
        Promotions promotion = promotionMapper.toEntity(promotionDTO);
        Promotions savedPromotion = promotionRepository.save(promotion);
        return promotionMapper.toDTO(savedPromotion);
    }

    @Override
    @Transactional
    public PromotionDTO updatePromotion(Long staffId, Long promotionId, PromotionDTO promotionDTO) {
        validateStaff(staffId);
        validatePromotionDates(promotionDTO);
        Promotions promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại với id: " + promotionId));
        if (!promotion.getCode().equals(promotionDTO.getCode()) &&
                promotionRepository.existsByCode(promotionDTO.getCode())) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại: " + promotionDTO.getCode());
        }
        promotion.setCode(promotionDTO.getCode());
        promotion.setDiscountPercentage(promotionDTO.getDiscountPercentage());
        promotion.setMinimumOrderValue(promotionDTO.getMinimumOrderValue());
        promotion.setMaxUsage(promotionDTO.getMaxUsage());
        promotion.setStartDate(promotionDTO.getStartDate());
        promotion.setEndDate(promotionDTO.getEndDate());
        promotion.setIsActive(promotionDTO.getIsActive() != null ? promotionDTO.getIsActive() : promotion.getIsActive());
        Promotions updatedPromotion = promotionRepository.save(promotion);
        return promotionMapper.toDTO(updatedPromotion);
    }

    @Override
    @Transactional
    public void deletePromotion(Long staffId, Long promotionId) {
        validateStaff(staffId);
        Promotions promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại với id: " + promotionId));
        promotionRepository.delete(promotion);
    }

    @Override
    public PromotionDTO getPromotionById(Long promotionId) {
        Promotions promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại với id: " + promotionId));
        return promotionMapper.toDTO(promotion);
    }

    @Override
    public List<PromotionDTO> getAllPromotions() {
        List<Promotions> promotions = promotionRepository.findAll();
        return promotionMapper.toDTOList(promotions);
    }

    @Override
    public Page<PromotionDTO> getPromotions(String code, Boolean isActive,
                                            LocalDateTime dateFrom, LocalDateTime dateTo,
                                            Pageable pageable) {
        // Kiểm tra tính hợp lệ của khoảng thời gian
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom phải trước hoặc bằng dateTo");
        }

        // Gọi native query từ repository
        Page<Promotions> promotionsPage = promotionRepository.findPromotions(
                code, isActive, dateFrom, dateTo, pageable);
        return promotionsPage.map(promotionMapper::toDTO);
    }

    private void validateStaff(Long staffId) {
        userRepository.findById(staffId)
                .filter(user -> user.getRole() == UserRole.STAFF)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or not a staff with id: " + staffId));
    }

    private void validatePromotionDates(PromotionDTO promotionDTO) {
        if (promotionDTO.getStartDate() != null && promotionDTO.getEndDate() != null && promotionDTO.getEndDate().isBefore(promotionDTO.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
