package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.ProductResponse;
import com.shopsphere.ecommerce.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {


//    @GetMapping("/api/v1/products/{id}")
//    public String getProduct(@PathVariable int id) {
//        return "Product ID : " + id;
//    }
//
//    @GetMapping("/api/v1/products")
//    public String getCategory(@RequestParam(required = false) String category) {
//
//        if (category == null) {
//            return "list of products";
//        } else {
//            return category;
//        }
//    }


    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/v1/products/{id}")
    public ProductResponse getProduct(@PathVariable int id) {
        return productService.getProduct(id);
    }

}