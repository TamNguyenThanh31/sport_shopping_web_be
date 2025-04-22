package com.runner.shopping.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runner.shopping.model.dto.ProductDTO;
import com.runner.shopping.service.ProductService;
import com.runner.shopping.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductDTO> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "isPrimaryFlags", required = false) String isPrimaryFlagsJson,
            Authentication authentication) {
        try {
            // Kiểm tra xác thực
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
            }
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            // Parse product JSON
            ProductDTO productDTO = objectMapper.readValue(productJson, ProductDTO.class);
            productDTO.setAddedById(userId); // Gán userId từ authentication
            log.info("Parsed product: {}", productDTO);

            // Parse isPrimaryFlags JSON
            List<Boolean> isPrimaryFlags;
            if (isPrimaryFlagsJson != null && !isPrimaryFlagsJson.trim().isEmpty()) {
                isPrimaryFlags = objectMapper.readValue(isPrimaryFlagsJson, new TypeReference<List<Boolean>>() {});
            } else {
                // Nếu không gửi isPrimaryFlags, mặc định tất cả là false, trừ ảnh đầu tiên là true
                isPrimaryFlags = imageFiles != null && !imageFiles.isEmpty()
                        ? Collections.nCopies(imageFiles.size(), false)
                        : Collections.emptyList();
                if (!isPrimaryFlags.isEmpty()) {
                    isPrimaryFlags.set(0, true); // Ảnh đầu tiên là primary
                }
            }
            log.info("Parsed isPrimaryFlags: {}", isPrimaryFlags);

            // Gọi service
            ProductDTO createdProduct = productService.createProduct(productDTO, imageFiles, isPrimaryFlags);
            return ResponseEntity.ok(createdProduct);
        } catch (Exception e) {
            log.error("Failed to parse product JSON or isPrimaryFlags", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product JSON or isPrimaryFlags format", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        try {
            List<ProductDTO> products = productService.getAllProducts();
            log.info("Retrieved {} products", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Failed to retrieve products", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve products", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        try {
            ProductDTO product = productService.getProductById(id);
            log.info("Retrieved product with ID: {}", id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("Failed to retrieve product with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDTO productDTO,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
            }
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);
            productDTO.setAddedById(userId);

            ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
            log.info("Updated product with ID: {}", id);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            log.error("Failed to update product with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update product", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            log.info("Deleted product with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete product with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        try {
            List<ProductDTO> products = productService.searchProducts(keyword);
            log.info("Found {} products matching keyword: {}", products.size(), keyword);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Failed to search products with keyword: {}", keyword, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search products", e);
        }
    }
}