package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Product;
import com.runner.shopping.entity.ProductImage;
import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.mapper.ProductMapper;
import com.runner.shopping.model.dto.ProductDTO;
import com.runner.shopping.model.dto.ProductVariantDTO;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.LocalStorageService;
import com.runner.shopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final LocalStorageService localStorageService;

    @Transactional
    @Override
    public ProductDTO createProduct(ProductDTO productDTO, List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        validateCategory(productDTO.getCategoryId());
        validateUser(productDTO.getAddedById());
        validateImageInputs(imageFiles, isPrimaryFlags);

        // Save product
        Product product = productMapper.toEntity(productDTO);
        product = productRepository.save(product);
        log.info("Created product with ID: {}", product.getId());

        // Save variants
        saveVariants(product.getId(), productDTO.getVariants());

        // Save images
        saveImages(product.getId(), imageFiles, isPrimaryFlags);

        // Lấy lại product và ánh xạ đầy đủ
        Product savedProduct = findProductById(product.getId());
        ProductDTO result = productMapper.toDTO(savedProduct);
        result.setVariants(productMapper.toVariantDTOList(
                productVariantRepository.findByProductIdNotDeleted(product.getId())
        ));
        result.setImages(productMapper.toImageDTOList(
                productImageRepository.findByProductIdNotDeleted(product.getId())
        ));

        return result;
    }


    @Override
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAllNotDeleted();
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());

        // Lấy tất cả variants và images theo productIds
        List<ProductVariant> variants = productVariantRepository.findByProductIdNotDeleted(productIds);
        List<ProductImage> images = productImageRepository.findByProductIdInNotDeleted(productIds);

        // Nhóm variants và images theo productId
        Map<Long, List<ProductVariant>> variantMap = variants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));
        Map<Long, List<ProductImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        // Ánh xạ sang DTO
        return products.stream()
                .map(product -> {
                    ProductDTO dto = productMapper.toDTO(product);
                    dto.setVariants(productMapper.toVariantDTOList(
                            variantMap.getOrDefault(product.getId(), List.of())
                    ));
                    dto.setImages(productMapper.toImageDTOList(
                            imageMap.getOrDefault(product.getId(), List.of())
                    ));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = findProductById(id);
        ProductDTO dto = productMapper.toDTO(product);
        dto.setVariants(productMapper.toVariantDTOList(
                productVariantRepository.findByProductIdNotDeleted(id)
        ));
        dto.setImages(productMapper.toImageDTOList(
                productImageRepository.findByProductIdNotDeleted(id)
        ));
        return dto;
    }

    @Transactional
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = findProductById(id);
        validateCategory(productDTO.getCategoryId());
        validateUser(productDTO.getAddedById());

        // Update product
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setBrand(productDTO.getBrand());
        product.setAddedBy(productDTO.getAddedById());
        product.setActive(productDTO.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        ProductDTO dto = productMapper.toDTO(product);
        dto.setVariants(productMapper.toVariantDTOList(
                productVariantRepository.findByProductIdNotDeleted(id)
        ));
        dto.setImages(productMapper.toImageDTOList(
                productImageRepository.findByProductIdNotDeleted(id)
        ));
        return dto;
    }

    @Transactional
    @Override
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setDeleted(1);
        productRepository.save(product);

        // Soft delete variants
        List<ProductVariant> variants = productVariantRepository.findByProductIdNotDeleted(id);
        variants.forEach(v -> v.setDeleted(1));
        productVariantRepository.saveAll(variants);

        // Soft delete images and delete from storage
        List<ProductImage> images = productImageRepository.findByProductIdNotDeleted(id);
        images.forEach(image -> {
            try {
                localStorageService.deleteImage(image.getImageUrl());
            } catch (IOException e) {
                log.error("Failed to delete image: {}", image.getImageUrl(), e);
                throw new RuntimeException("Failed to delete image: " + image.getImageUrl(), e);
            }
            image.setDeleted(1);
        });
        productImageRepository.saveAll(images);
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameOrBrand(keyword).stream()
                .map(product -> {
                    ProductDTO dto = productMapper.toDTO(product);
                    dto.setVariants(productMapper.toVariantDTOList(
                            productVariantRepository.findByProductIdNotDeleted(product.getId())
                    ));
                    dto.setImages(productMapper.toImageDTOList(
                            productImageRepository.findByProductIdNotDeleted(product.getId())
                    ));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void validateCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new IllegalArgumentException("Category not found or deleted"));
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateImageInputs(List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        if (isPrimaryFlags != null && isPrimaryFlags.size() != imageFiles.size()) {
            throw new IllegalArgumentException("Number of primary flags must match number of images");
        }
        // Ensure at least one image is primary
        boolean hasPrimary = isPrimaryFlags != null && isPrimaryFlags.contains(true);
        if (!hasPrimary && !imageFiles.isEmpty()) {
            log.warn("No primary image specified; setting first image as primary");
            isPrimaryFlags.set(0, true);
        }
    }

    private void saveVariants(Long productId, List<ProductVariantDTO> variantDTOs) {
        if (variantDTOs == null || variantDTOs.isEmpty()) {
            log.warn("No variants provided for product {}", productId);
            return;
        }
        log.info("Saving {} variants for product {}", variantDTOs.size(), productId);
        List<ProductVariant> variants = variantDTOs.stream()
                .map(productMapper::toEntity)
                .peek(v -> v.setProductId(productId))
                .collect(Collectors.toList());
        productVariantRepository.saveAll(variants); // Lưu hàng loạt
    }

    private void saveImages(Long productId, List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            log.warn("No images provided for product {}", productId);
            return;
        }
        log.info("Processing {} images for product {}", imageFiles.size(), productId);
        for (int i = 0; i < imageFiles.size(); i++) {
            MultipartFile file = imageFiles.get(i);
            boolean isPrimary = isPrimaryFlags != null && i < isPrimaryFlags.size() && isPrimaryFlags.get(i);

            try {
                String imageUrl = localStorageService.uploadImage(file, productId);
                ProductImage image = new ProductImage();
                image.setProductId(productId);
                image.setImageUrl(imageUrl);
                image.setFileName(file.getOriginalFilename());
                image.setPrimary(isPrimary);
                image.setCreatedAt(LocalDateTime.now());
                image.setDeleted(0);
                productImageRepository.save(image);
                log.info("Saved image {} for product {}", file.getOriginalFilename(), productId);
            } catch (IOException e) {
                log.error("Failed to save image: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to save image: " + file.getOriginalFilename(), e);
            }
        }
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .filter(p -> p.getDeleted() == 0)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or deleted"));
    }
}