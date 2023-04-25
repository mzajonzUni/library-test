package pl.zajonz.librarytest.user;

import pl.zajonz.librarytest.user.model.User;

import java.util.List;

public interface UserService {

    User create(User user);

    List<User> getAll(int pageNo, int pageSize);
}
