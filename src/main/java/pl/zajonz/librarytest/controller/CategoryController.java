package pl.zajonz.librarytest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.zajonz.librarytest.adnotation.MonitorMethod;
import pl.zajonz.librarytest.mapper.CategoryMapper;
import pl.zajonz.librarytest.model.DTO.CategoryDto;
import pl.zajonz.librarytest.service.CategoryServiceImpl;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryServiceImpl categoryServiceImpl;
    private final CategoryMapper mapper;

    @GetMapping
    @MonitorMethod
    public List<CategoryDto> getCategories() {
        return categoryServiceImpl.getCategories()
                .stream()
                .map(mapper::fromCategoryEntity)
                .toList();
    }

    @PatchMapping("/{id}/subscribe")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @MonitorMethod
    public void subscribe(Principal principal, @PathVariable int id) {
        categoryServiceImpl.subscribe(principal.getName(), id);
    }

}
