package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.ProductRequest;
import com.shopsphere.ecommerce.dto.ProductResponse;
import com.shopsphere.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

//    @GetMapping
//    public Page<ProductResponse> getAllProducts(Pageable pageable) {
//        return productService.getAllProducts(pageable);
//    }


    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable
    ) {
        return productService.filterProducts(
                keyword,
                minPrice,
                maxPrice,
                active,
                inStock,
                categoryId,
                pageable
        );
    }


    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/search")
//    public Page<ProductResponse> searchProducts(
//            @RequestParam String keyword, Pageable pageable) {
//        return productService.searchProduct(keyword, pageable);
//    }

//    @GetMapping("/filter/price")
//    public Page<ProductResponse> getProductsByPrice(
//            @RequestParam BigDecimal minPrice,
//            @RequestParam BigDecimal maxPrice,
//            Pageable pageable
//    ) {
//        return productService.getProductsByPrice(minPrice, maxPrice, pageable);
//    }

//    @GetMapping("/filter")
//    public Page<ProductResponse> filterProducts(
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) BigDecimal minPrice,
//            @RequestParam(required = false) BigDecimal maxPrice,
//            @RequestParam(required = false) Boolean active,
//            @RequestParam(required = false) Boolean inStock,
//            Pageable pageable
//    ) {
//        return productService.filterProducts(
//                keyword,
//                minPrice,
//                maxPrice,
//                active,
//                inStock,
//                pageable
//        );
//    }

}