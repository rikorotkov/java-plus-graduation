package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto save(NewCategoryDto dto);

    void deleteCategory(Long catId);

    CategoryDto update(CategoryDto dto);

    List<CategoryDto> getAllCategories(Pageable pageable);

    CategoryDto getCategoryById(Long id);
}
