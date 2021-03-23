package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.security.ShaPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MockUsers implements DisposableBean {

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
    private final List<User> createdUsers = new ArrayList<>();
    private int userIndex = 1;

    @WithTenant("integration-test")
    public User createSimpleUser(String username, String password) {
        return createUser(username, password, User.Role.U, null);
    }

    @WithTenant("integration-test")
    public User createAdminUser(String username, String password) {
        return createUser(username, password, User.Role.A, null);
    }

    @WithTenant("integration-test")
    public User createFriendUser(String username, String password, User referenceUser) {
        return createUser(username, password, User.Role.S, referenceUser);
    }

    private User createUser(String username, String password, User.Role role, User referenceUser) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.name());
        user.setEnabled(true);
        user.setFriendReferral(referenceUser);
        user.setPosition(userIndex++);

        User storedUser = userRepo.save(user);
        createdUsers.add(storedUser);

        return storedUser;
    }

    @WithTenant("integration-test")
    public void deleteUsers() {
        createdUsers.sort(Comparator.comparing(User::getRole).reversed());
        createdUsers.forEach(userRepo::delete);
    }

    @Override
    @WithTenant("integration-test")
    public void destroy() throws Exception {
        deleteUsers();
    }
}
