package pl.zajonz.librarytest.model.DTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryDto {

    private int id;
    private String name;

}
