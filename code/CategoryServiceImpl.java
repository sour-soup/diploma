package org.soursoup.bimbim.service.impl;

import org.soursoup.bimbim.dto.request.CategoryCreateRequest;
import org.soursoup.bimbim.entity.Category;
import org.soursoup.bimbim.exception.NotFoundException;
import org.soursoup.bimbim.mapper.CategoryCreateMapper;
import org.soursoup.bimbim.repository.CategoryRepository;
import org.soursoup.bimbim.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryCreateMapper categoryCreateMapper;

    @Override
    public Category createCategory(CategoryCreateRequest categoryCreateRequest) {

        Category category = categoryCreateMapper.toEntity(categoryCreateRequest);
        return categoryRepository.save(category);
    }

    @Override
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Catefory does not exist"));
    }

    @Override
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public void deleteCategory(Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
        } else {
            throw new NotFoundException("Category does not exist.");
        }
    }
}
