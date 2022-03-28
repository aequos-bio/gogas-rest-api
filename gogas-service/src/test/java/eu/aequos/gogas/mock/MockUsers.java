package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.security.ShaPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@WithTenant("integration-test")
public class MockUsers {

    public static final String SIMPLE_USER_USERNAME = "simple_user";
    public static final String SIMPLE_USER_PASSWORD = "simple_user";

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
    private final List<User> createdUsers = new ArrayList<>();
    private int userIndex = 1;

    public User createSimpleUser(String username, String password, String firstName, String lastName) {
        return createUser(username, password, firstName, lastName, User.Role.U, null);
    }

    public User createAdminUser(String username, String password, String firstName, String lastName) {
        return createUser(username, password, firstName, lastName, User.Role.A, null);
    }

    public User createFriendUser(String username, String password, String firstName, String lastName, User referenceUser) {
        return createUser(username, password, firstName, lastName, User.Role.S, referenceUser);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public void deleteByUsername(String username) {
        userRepo.findByUsername(username)
                .ifPresent(userRepo::delete);
    }

    private User createUser(String username, String password, String firstName, String lastName,
                            User.Role role, User referenceUser) {

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role.name());
        user.setEnabled(true);
        user.setFriendReferral(referenceUser);
        user.setPosition(userIndex++);

        User storedUser = userRepo.save(user);
        createdUsers.add(storedUser);

        return storedUser;
    }

    public String getSimpleUserId() {
        return createdUsers.get(0).getId();
    }

    public void deleteUsers() {
        createdUsers.sort(Comparator.comparing(User::getRole).reversed());
        createdUsers.forEach(userRepo::delete);
    }

    public void init() {
        createSimpleUser(SIMPLE_USER_USERNAME, SIMPLE_USER_PASSWORD, "Simple", "User");
    }

    public void destroy() {
        deleteUsers();
    }
}
