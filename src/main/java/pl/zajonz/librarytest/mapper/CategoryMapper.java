package pl.zajonz.librarytest.mapper;

import org.mapstruct.Mapper;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.DTO.CategoryDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto fromCategoryEntity(Category category);

}
