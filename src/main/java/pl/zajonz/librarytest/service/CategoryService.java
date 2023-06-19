package pl.zajonz.librarytest.service;

import pl.zajonz.librarytest.model.Category;

import java.util.List;

public interface CategoryService {

    void subscribe(String name, int id);

    List<Category> getCategories();

}
