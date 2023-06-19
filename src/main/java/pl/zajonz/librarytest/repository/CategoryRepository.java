package pl.zajonz.librarytest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zajonz.librarytest.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
