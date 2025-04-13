package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Product;
import com.runner.shopping.entity.ProductImage;
import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.mapper.ProductMapper;
import com.runner.shopping.model.dto.ProductDTO;
import com.runner.shopping.model.dto.ProductImageDTO;
import com.runner.shopping.model.dto.ProductVariantDTO;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        // Kiểm tra categoryId
        categoryRepository.findById(productDTO.getCategoryId())
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Category not found or deleted"));

        // Kiểm tra addedById
        userRepository.findById(productDTO.getAddedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Chuyển DTO sang Entity và lưu sản phẩm
        Product product = productMapper.toEntity(productDTO);
        product = productRepository.save(product);

        // Lưu variants
        log.info("Variants received: {}", productDTO.getVariants());
        if (productDTO.getVariants() != null && !productDTO.getVariants().isEmpty()) {
            for (ProductVariantDTO variantDTO : productDTO.getVariants()) {
                log.info("Processing variant: {}", variantDTO);
                ProductVariant variant = productMapper.toEntity(variantDTO);
                variant.setProductId(product.getId());
                productVariantRepository.save(variant);
            }
        } else {
            log.warn("No variants provided in the request");
        }

        // Lưu images
        log.info("Images received: {}", productDTO.getImages());
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            for (ProductImageDTO imageDTO : productDTO.getImages()) {
                log.info("Processing image: {}", imageDTO);
                ProductImage image = productMapper.toEntity(imageDTO);
                image.setProductId(product.getId());
                productImageRepository.save(image);
            }
        } else {
            log.warn("No images provided in the request");
        }

        // Cập nhật DTO với dữ liệu từ database
        productDTO.setId(product.getId());
        productDTO.setVariants(productVariantRepository.findByProductIdNotDeleted(product.getId())
                .stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList()));
        productDTO.setImages(productImageRepository.findByProductIdNotDeleted(product.getId())
                .stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList()));

        return productDTO;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAllNotDeleted().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Product not found or deleted"));
        return mapToDTO(product);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Product not found or deleted"));

        // Kiểm tra categoryId
        categoryRepository.findById(productDTO.getCategoryId())
                .filter(c -> c.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Category not found or deleted"));

        // Kiểm tra addedById
        userRepository.findById(productDTO.getAddedById())
//                .filter(u -> u.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("User not found or deleted"));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setBrand(productDTO.getBrand());
        product.setAddedBy(productDTO.getAddedById());
        product.setActive(productDTO.isActive());
        productRepository.save(product);
        return productDTO;
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("Product not found or deleted"));
        product.setDeleted(1);
        productRepository.save(product);

        // Xóa mềm biến thể
        List<ProductVariant> variants = productVariantRepository.findByProductIdNotDeleted(id);
        variants.forEach(v -> v.setDeleted(1));
        productVariantRepository.saveAll(variants);

        // Xóa mềm hình ảnh
        List<ProductImage> images = productImageRepository.findByProductIdNotDeleted(id);
        images.forEach(i -> i.setDeleted(1));
        productImageRepository.saveAll(images);
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameOrBrand(keyword).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ProductDTO mapToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategoryId());
        dto.setBrand(product.getBrand());
        dto.setAddedById(product.getAddedBy());
        dto.setActive(product.isActive());

        // Lấy biến thể
        List<ProductVariantDTO> variants = productVariantRepository.findByProductIdNotDeleted(product.getId())
                .stream().map(variant -> {
                    ProductVariantDTO variantDTO = new ProductVariantDTO();
                    variantDTO.setId(variant.getId());
                    variantDTO.setProductId(variant.getProductId());
                    variantDTO.setSize(variant.getSize());
                    variantDTO.setColor(variant.getColor());
                    variantDTO.setStock(variant.getStock());
                    variantDTO.setPrice(variant.getPrice());
                    variantDTO.setSku(variant.getSku());
                    return variantDTO;
                }).collect(Collectors.toList());
        dto.setVariants(variants);

        // Lấy hình ảnh
        List<ProductImageDTO> images = productImageRepository.findByProductIdNotDeleted(product.getId())
                .stream().map(image -> {
                    ProductImageDTO imageDTO = new ProductImageDTO();
                    imageDTO.setId(image.getId());
                    imageDTO.setProductId(image.getProductId());
                    imageDTO.setImageUrl(image.getImageUrl());
                    imageDTO.setPrimary(image.isPrimary());
                    return imageDTO;
                }).collect(Collectors.toList());
        dto.setImages(images);

        return dto;
    }
}
