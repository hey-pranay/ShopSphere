package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.CategoryRequest;
import com.shopsphere.ecommerce.dto.CategoryResponse;
import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
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

        category.setName(request.getName());

        Category updateCategory = categoryRepository.save(category);

        return new CategoryResponse(
                updateCategory.getId(),
                updateCategory.getName()
        );
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with id " + id + "not found"
                ));

        categoryRepository.delete(category);

    }

}