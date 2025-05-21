package com.runner.shopping.controller;

import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionDTO> createPromotion(@RequestParam Long staffId, @Valid @RequestBody PromotionDTO promotionDTO) {
        PromotionDTO createdPromotion = promotionService.createPromotion(staffId, promotionDTO);
        return ResponseEntity.ok(createdPromotion);
    }

    @PutMapping("/{promotionId}")
    public ResponseEntity<PromotionDTO> updatePromotion(@RequestParam Long staffId, @PathVariable Long promotionId, @Valid @RequestBody PromotionDTO promotionDTO) {
        PromotionDTO updatedPromotion = promotionService.updatePromotion(staffId, promotionId, promotionDTO);
        return ResponseEntity.ok(updatedPromotion);
    }

    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> deletePromotion(@RequestParam Long staffId, @PathVariable Long promotionId) {
        promotionService.deletePromotion(staffId, promotionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{promotionId}")
    public ResponseEntity<PromotionDTO> getPromotionById(@PathVariable Long promotionId) {
        PromotionDTO promotionDTO = promotionService.getPromotionById(promotionId);
        return ResponseEntity.ok(promotionDTO);
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> getAllPromotions() {
        List<PromotionDTO> promotions = promotionService.getAllPromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PromotionDTO>> getPromotions(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            Pageable pageable) {
        Page<PromotionDTO> promotions = promotionService.getPromotions(
                code, isActive, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(promotions);
    }
}
