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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // Lưu sản phẩm
        Product product = productMapper.toEntity(productDTO);
        product = productRepository.save(product);
        log.info("Đã tạo sản phẩm với ID: {}", product.getId());

        // Lưu biến thể
        saveVariants(product.getId(), productDTO.getVariants());

        // Lưu hình ảnh
        saveImages(product.getId(), imageFiles, isPrimaryFlags);

        // Lấy sản phẩm đã lưu và ánh xạ sang DTO
        Product savedProduct = findProductById(product.getId());
        return mapToProductDTO(savedProduct);
    }

    @Transactional
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO, List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        Product product = findProductById(id);
        validateCategory(productDTO.getCategoryId());
        validateUser(productDTO.getAddedById());

        // Cập nhật thông tin sản phẩm
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setBrand(productDTO.getBrand());
        product.setAddedBy(productDTO.getAddedById());
        product.setActive(productDTO.isActive());
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Đã cập nhật thông tin sản phẩm với ID: {}", id);

        // Cập nhật biến thể (xóa mềm cũ, lưu mới)
        if (productDTO.getVariants() != null) {
            List<ProductVariant> existingVariants = productVariantRepository.findByProductId(id);
            existingVariants.forEach(variant -> variant.setDeleted(1));
            productVariantRepository.saveAll(existingVariants);
            saveVariants(id, productDTO.getVariants());
            log.info("Đã cập nhật biến thể cho sản phẩm ID: {}", id);
        }

        // Cập nhật hình ảnh (xóa mềm cũ, lưu mới)
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<ProductImage> existingImages = productImageRepository.findByProductIdNotDeleted(id);
            existingImages.forEach(image -> {
                try {
                    localStorageService.deleteImage(image.getImageUrl());
                    image.setDeleted(1);
                } catch (IOException e) {
                    log.error("Không thể xóa hình ảnh: {}", image.getImageUrl(), e);
                    throw new RuntimeException("Không thể xóa hình ảnh: " + image.getImageUrl(), e);
                }
            });
            productImageRepository.saveAll(existingImages);
            validateImageInputs(imageFiles, isPrimaryFlags);
            saveImages(id, imageFiles, isPrimaryFlags);
            log.info("Đã cập nhật hình ảnh cho sản phẩm ID: {}", id);
        }

        // Lấy sản phẩm đã cập nhật và ánh xạ sang DTO
        Product updatedProduct = findProductById(id);
        return mapToProductDTO(updatedProduct);
    }

    @Override
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAllNotDeleted(pageable);
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        List<ProductVariant> variants = productVariantRepository.findByProductIdsNotDeleted(productIds);
        List<ProductImage> images = productImageRepository.findByProductIdInNotDeleted(productIds);

        Map<Long, List<ProductVariant>> variantMap = variants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));
        Map<Long, List<ProductImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        return productPage.map(product -> {
            ProductDTO dto = productMapper.toDTO(product);
            dto.setVariants(productMapper.toVariantDTOList(
                    variantMap.getOrDefault(product.getId(), List.of())
            ));
            dto.setImages(productMapper.toImageDTOList(
                    imageMap.getOrDefault(product.getId(), List.of())
            ));
            return dto;
        });
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = findProductById(id);
        return mapToProductDTO(product);
    }

    @Transactional
    @Override
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setDeleted(1);
        productRepository.save(product);

        List<ProductVariant> variants = productVariantRepository.findByProductIdNotDeleted(id);
        variants.forEach(v -> v.setDeleted(1));
        productVariantRepository.saveAll(variants);

        List<ProductImage> images = productImageRepository.findByProductIdNotDeleted(id);
        images.forEach(image -> {
            try {
                localStorageService.deleteImage(image.getImageUrl());
            } catch (IOException e) {
                log.error("Không thể xóa hình ảnh: {}", image.getImageUrl(), e);
                throw new RuntimeException("Không thể xóa hình ảnh: " + image.getImageUrl(), e);
            }
            image.setDeleted(1);
        });
        productImageRepository.saveAll(images);
    }

    @Override
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.searchByNameOrBrand(keyword, pageable);
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        List<ProductVariant> variants = productVariantRepository.findByProductIdsNotDeleted(productIds);
        List<ProductImage> images = productImageRepository.findByProductIdInNotDeleted(productIds);

        Map<Long, List<ProductVariant>> variantMap = variants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));
        Map<Long, List<ProductImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        return productPage.map(product -> {
            ProductDTO dto = productMapper.toDTO(product);
            dto.setVariants(productMapper.toVariantDTOList(
                    variantMap.getOrDefault(product.getId(), List.of())
            ));
            dto.setImages(productMapper.toImageDTOList(
                    imageMap.getOrDefault(product.getId(), List.of())
            ));
            return dto;
        });
    }

    private void validateCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại hoặc đã bị xóa"));
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
    }

    private void validateImageInputs(List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một hình ảnh");
        }
        if (isPrimaryFlags != null && isPrimaryFlags.size() != imageFiles.size()) {
            throw new IllegalArgumentException("Số lượng cờ chính phải khớp với số lượng hình ảnh");
        }
        boolean hasPrimary = isPrimaryFlags != null && isPrimaryFlags.contains(true);
        if (!hasPrimary && !imageFiles.isEmpty()) {
            log.warn("Không có hình ảnh chính được chỉ định; đặt hình ảnh đầu tiên làm chính");
            isPrimaryFlags.set(0, true);
        }
    }

    private void saveVariants(Long productId, List<ProductVariantDTO> variantDTOs) {
        if (variantDTOs == null || variantDTOs.isEmpty()) {
            log.warn("Không có biến thể nào được cung cấp cho sản phẩm {}", productId);
            return;
        }
        log.info("Lưu {} biến thể cho sản phẩm {}", variantDTOs.size(), productId);
        List<ProductVariant> variants = variantDTOs.stream()
                .map(productMapper::toEntity)
                .peek(v -> v.setProductId(productId))
                .collect(Collectors.toList());
        productVariantRepository.saveAll(variants);
    }

    private void saveImages(Long productId, List<MultipartFile> imageFiles, List<Boolean> isPrimaryFlags) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            log.warn("Không có hình ảnh nào được cung cấp cho sản phẩm {}", productId);
            return;
        }
        log.info("Xử lý {} hình ảnh cho sản phẩm {}", imageFiles.size(), productId);
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
                log.info("Đã lưu hình ảnh {} cho sản phẩm {}", file.getOriginalFilename(), productId);
            } catch (IOException e) {
                log.error("Không thể lưu hình ảnh: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Không thể lưu hình ảnh: " + file.getOriginalFilename(), e);
            }
        }
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .filter(p -> p.getDeleted() == 0)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã bị xóa"));
    }

    private ProductDTO mapToProductDTO(Product product) {
        ProductDTO dto = productMapper.toDTO(product);
        dto.setVariants(productMapper.toVariantDTOList(
                productVariantRepository.findByProductIdNotDeleted(product.getId())
        ));
        dto.setImages(productMapper.toImageDTOList(
                productImageRepository.findByProductIdNotDeleted(product.getId())
        ));
        return dto;
    }
}