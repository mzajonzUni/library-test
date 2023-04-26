package pl.zajonz.librarytest.user;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.user.model.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User create(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User with username: " + user.getUsername() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public Page<User> getAll(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return userRepository.findAll(pageable);
    }

    @Override
    public List<Book> getAllBooks(String name, String role, int id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found user with id: " + id));

        if (!role.equals("[ROLE_EMPLOYEE]") && !user.getUsername().equals(name)) {
            throw new IllegalArgumentException("No access to book with id: " + id);
        }

        return user.getBooks().stream().toList();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));
    }

}
