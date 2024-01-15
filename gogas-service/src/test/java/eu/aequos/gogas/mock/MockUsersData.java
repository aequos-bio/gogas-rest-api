package eu.aequos.gogas.mock;

import eu.aequos.gogas.mvc.WithTenant;
import eu.aequos.gogas.persistence.entity.NotificationPreferences;
import eu.aequos.gogas.persistence.entity.PushToken;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.NotificationPreferencesRepo;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.security.ShaPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
@WithTenant("integration-test")
public class MockUsersData implements MockDataLifeCycle {

    public static final String ADMIN_USERNAME = "admin";
    public static final String SIMPLE_USER_USERNAME = "simple_user";
    public static final String SIMPLE_USER_PASSWORD = "simple_user";

    private final UserRepo userRepo;
    private final NotificationPreferencesRepo notificationPreferencesRepo;
    private final PushTokenRepo pushTokenRepo;

    private final PasswordEncoder passwordEncoder = new ShaPasswordEncoder();
    private final List<User> createdUsers = new ArrayList<>();
    private int userIndex = 1;

    public User createSimpleUser(String username, String password, String firstName, String lastName) {
        return createUser(username, password, firstName, lastName, User.Role.U, null, true);
    }

    public User createAdminUser(String username, String password, String firstName, String lastName) {
        return createUser(username, password, firstName, lastName, User.Role.A, null, true);
    }

    public User createFriendUser(String username, String password, String firstName, String lastName, User referenceUser) {
        return createUser(username, password, firstName, lastName, User.Role.S, referenceUser, true);
    }

    public User createDisabledUser(String username, String password, String firstName, String lastName) {
        return createUser(username, password, firstName, lastName, User.Role.U, null, false);
    }

    public User createDisabledFriend(String username, String password, String firstName, String lastName, User referenceUser) {
        return createUser(username, password, firstName, lastName, User.Role.S, referenceUser, false);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Transactional
    public void deleteByUsername(String username) {
        userRepo.findByUsername(username)
                .ifPresent(entity -> {
                    notificationPreferencesRepo.deleteByUserId(entity.getId());
                    userRepo.delete(entity);
                });
    }

    private User createUser(String username, String password, String firstName, String lastName,
                            User.Role role, User referenceUser, boolean enabled) {

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role.name());
        user.setEnabled(enabled);
        user.setFriendReferral(referenceUser);
        user.setPosition(userIndex++);

        User storedUser = userRepo.save(user);
        createdUsers.add(storedUser);

        NotificationPreferences notificationPreferences = new NotificationPreferences();
        notificationPreferences.setUserId(storedUser.getId());
        notificationPreferences.setOnOrderOpened(true);
        notificationPreferences.setOnOrderExpiration(true);
        notificationPreferences.setOnOrderDelivery(true);
        notificationPreferences.setOnOrderUpdatedQuantity(true);
        notificationPreferences.setOnOrderAccounted(true);

        notificationPreferencesRepo.save(notificationPreferences);

        return storedUser;
    }

    public void addPushNotificationToken(String userId, String token) {
        PushToken pushToken = new PushToken();
        pushToken.setUserId(userId);
        pushToken.setDeviceId(token + "_device");
        pushToken.setToken(token);

        pushTokenRepo.save(pushToken);
    }

    public String getSimpleUserId() {
        return createdUsers.get(0).getId();
    }

    public String getDefaultAdminId() {
        return userRepo.findByUsername(ADMIN_USERNAME).stream()
                .findFirst()
                .map(User::getId)
                .orElse(null);
    }

    public void deleteUsers() {
        pushTokenRepo.deleteAll();
        notificationPreferencesRepo.deleteAll();

        createdUsers.sort(Comparator.comparing(User::getRole));
        createdUsers.forEach(userRepo::delete);
        createdUsers.clear();
    }

    public Set<String> getAllUsers(boolean excludeFriends, boolean activeOnly) {
        return getAllUsersAttribute(excludeFriends, activeOnly, User::getUsername);
    }

    public Set<String> getAllUserIds(boolean excludeFriends, boolean activeOnly) {
        return getAllUsersAttribute(excludeFriends, activeOnly, user -> user.getId().toUpperCase());
    }

    private Set<String> getAllUsersAttribute(boolean excludeFriends, boolean activeOnly, Function<User, String> attributeExtractor) {
        return createdUsers.stream()
                .filter(not(user -> excludeFriends && user.getRoleEnum() == User.Role.S))
                .filter(not(user -> activeOnly && !user.isEnabled()))
                .map(attributeExtractor)
                .collect(Collectors.toSet());
    }

    @Override
    public void init() {
        deleteNotDefaultAdmin();
        createSimpleUser(SIMPLE_USER_USERNAME, SIMPLE_USER_PASSWORD, "Simple", "User");
    }

    private void deleteNotDefaultAdmin() {
        List<User> collect = userRepo.findAll().stream()
                .sorted(Comparator.comparing(User::getRole))
                .filter(user -> !ADMIN_USERNAME.equals(user.getUsername()))
                .collect(Collectors.toList());

        collect.forEach(userRepo::delete);
    }

    @Override
    public void destroy() {
        deleteUsers();
    }
}
