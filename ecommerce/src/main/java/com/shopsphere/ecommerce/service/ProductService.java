package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.ProductRequest;
import com.shopsphere.ecommerce.dto.ProductResponse;
import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with id " + id + " not found"
                ));

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductResponse createProduct(ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + request.getCategoryId() + " not found"
                ));

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getStockQuantity(),
                savedProduct.getActive(),
                savedProduct.getImageUrl(),
                savedProduct.getCategory().getId(),
                savedProduct.getCategory().getName(),
                savedProduct.getCreatedAt(),
                savedProduct.getUpdatedAt()

        );
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {

        Page<Product> products = productRepository.findAll(pageable);

        return products
                .map(product -> new ProductResponse(
                                product.getId(),
                                product.getName(),
                                product.getDescription(),
                                product.getPrice(),
                                product.getStockQuantity(),
                                product.getActive(),
                                product.getImageUrl(),
                                product.getCategory().getId(),
                                product.getCategory().getName(),
                                product.getCreatedAt(),
                                product.getUpdatedAt()
                        )
                );
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with id " + id + " not found"
                ));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + request.getCategoryId() + " not found"
                ));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);

        return new ProductResponse(
                updatedProduct.getId(),
                updatedProduct.getName(),
                updatedProduct.getDescription(),
                updatedProduct.getPrice(),
                updatedProduct.getStockQuantity(),
                updatedProduct.getActive(),
                updatedProduct.getImageUrl(),
                updatedProduct.getCategory().getId(),
                updatedProduct.getCategory().getName(),
                updatedProduct.getCreatedAt(),
                updatedProduct.getUpdatedAt()
        );
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with id " + id + " not found"
                ));


        productRepository.delete(product);
    }

    public Page<ProductResponse> searchProduct(
            String keyword, Pageable pageable
    ) {
        Page<Product> products = productRepository.findByNameContainingIgnoreCase(keyword, pageable);

        return products
                .map(product -> new ProductResponse(
                                product.getId(),
                                product.getName(),
                                product.getDescription(),
                                product.getPrice(),
                                product.getStockQuantity(),
                                product.getActive(),
                                product.getImageUrl(),
                                product.getCategory().getId(),
                                product.getCategory().getName(),
                                product.getCreatedAt(),
                                product.getUpdatedAt()
                        )
                );
    }

    public Page<ProductResponse> getProductsByPrice(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        Page<Product> products = productRepository.findByPriceBetween(
                minPrice,
                maxPrice,
                pageable
        );

        return products.map(product -> new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        ));


    }

    public Page<ProductResponse> filterProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active,
            Boolean inStock,
            Long categoryId,
            Pageable pageable
    ) {
        Specification<Product> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            specification = specification.and(
                    ProductSpecification.nameContains(keyword)
            );
        }

        if (minPrice != null) {
            specification = specification.and(
                    ProductSpecification.priceGreaterThanOrEqual(minPrice)
            );
        }

        if (maxPrice != null) {
            specification = specification.and(
                    ProductSpecification.priceLessThanOrEqual(maxPrice)
            );
        }

        if (active != null) {
            specification = specification.and(
                    ProductSpecification.isActive(active)
            );
        }

        if (inStock != null) {
            specification = specification.and(
                    ProductSpecification.isInStock(inStock)
            );
        }

        if (categoryId != null) {
            specification = specification.and(
                    ProductSpecification.hasCategory(categoryId)
            );
        }

        Page<Product> products =
                productRepository.findAll(specification, pageable);

        return products.map(product -> new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        ));

    }


}