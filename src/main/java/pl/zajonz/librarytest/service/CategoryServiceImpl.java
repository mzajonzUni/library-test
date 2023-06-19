package pl.zajonz.librarytest.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zajonz.librarytest.event.model.InfoEvent;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public void subscribe(String name, int categoryId) {
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new EntityNotFoundException("Not found user with username: " + name));

        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new EntityNotFoundException("Not found category with id: " + categoryId));

        user.getSubscribedCategories().add(category);

        publisher.publishEvent(new InfoEvent(user + " has subscribed category: " + category));

        userRepository.save(user);
    }

    @Override
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }
}
