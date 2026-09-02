package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.ProductRequest;
import com.shopsphere.ecommerce.dto.ProductResponse;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());
        product.setImageUrl(request.getImageUrl());

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getStockQuantity(),
                savedProduct.getActive(),
                savedProduct.getImageUrl(),
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
}