package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.CategoryRequest;
import com.shopsphere.ecommerce.dto.CategoryResponse;
import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.exception.CategoryInUseException;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;
import com.shopsphere.ecommerce.exception.DuplicateCategoryException;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateCategoryException(
                    "Category with name '" + request.getName() + "' already exists"
            );
        }

        Category category = new Category();

        category.setName(request.getName());

        Category savedCategory = categoryRepository.save(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName()
        );
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName()
                )).toList();
    }

    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + id + " not found"
                ));

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }

    public CategoryResponse updateCategory(
            Long id,
            CategoryRequest request
    ) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + id + " not found"
                ));

        if (categoryRepository.existsByNameIgnoreCase(request.getName())
                && !category.getName().equalsIgnoreCase(request.getName())
        ) {
            throw new DuplicateCategoryException(
                    "Category with name '" + request.getName() + "' already exists"
            );
        }

        category.setName(request.getName());

        Category updateCategory = categoryRepository.save(category);

        return new CategoryResponse(
                updateCategory.getId(),
                updateCategory.getName()
        );
    }

    public void deleteCategory(Long id) {

        if (productRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(
                    "Category with id " + id + " cannot be deleted because products are using it"
            );
        }

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + id + " not found"
                ));


        categoryRepository.delete(category);

    }

}