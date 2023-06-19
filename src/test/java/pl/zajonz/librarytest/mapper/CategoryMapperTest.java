package pl.zajonz.librarytest.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.DTO.CategoryDto;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
class CategoryMapperTest {

    @Autowired
    private CategoryMapper mapper;

    @Test
    void testFromCategoryEntity() {
        //given
        Category category = Category.builder()
                .id(1)
                .name("Test")
                .build();
        //when
        CategoryDto returned = mapper.fromCategoryEntity(category);

        //then
        assertEquals(category.getId(),returned.getId());
        assertEquals(category.getName(),returned.getName());
    }
}