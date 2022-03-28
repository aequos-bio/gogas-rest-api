package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.DuplicatedItemException;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.MissingOrInvalidParameterException;
import eu.aequos.gogas.notification.mail.MailNotificationSender;
import eu.aequos.gogas.persistence.entity.PushToken;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import eu.aequos.gogas.persistence.repository.PushTokenRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import eu.aequos.gogas.security.RandomPassword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class UserService extends CrudService<User, String> {

    private final String DISABLED_ICON = "<span class='glyphicon glyphicon-ban-circle' style='margin-right:10px'></span>";

    private ConfigurationService configurationService;
    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private MailNotificationSender mailNotificationSender;
    private PushTokenRepo pushTokenRepo;

    //TODO: cache users

    public UserService(ConfigurationService configurationService, UserRepo userRepo, PushTokenRepo pushTokenRepo,
                       PasswordEncoder passwordEncoder, MailNotificationSender mailNotificationSender) {
        super(userRepo, "user");

        this.configurationService = configurationService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailNotificationSender = mailNotificationSender;
        this.pushTokenRepo = pushTokenRepo;
    }

    public List<SelectItemDTO> getUsersForSelect(User.Role role, boolean withAll, String allLabel) {
        return toSelectItems(userRepo.findByRole(role.name(), UserCoreInfo.class), withAll, allLabel);
    }

    public User create(UserDTO dto) throws MissingOrInvalidParameterException {
        if (userRepo.findByUsername(dto.getUsername()).isPresent())
            throw new DuplicatedItemException("user", dto.getUsername());

        if (!dto.hasPassword())
            throw new MissingOrInvalidParameterException("Password is required");

        dto.setHashedPassword(encodePassword(dto.getPassword()));
        dto.setPosition(userRepo.getMaxUserPosition() + 1);
        return super.create(dto);
    }

    public User update(String s, UserDTO dto) throws ItemNotFoundException {
        dto.setHashedPassword(encodePassword(dto.getPassword()));
        return super.update(s, dto);
    }

    private String encodePassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty())
            return null;

        return passwordEncoder.encode(plainPassword);
    }

    public List<SelectItemDTO> getFriendsForSelect(String referralUserId, boolean withAll, boolean includeReferral) {
        List<SelectItemDTO> selectItems = new ArrayList<>();

        if (includeReferral)
            selectItems.add(getSelectItem(getRequired(referralUserId)));

        selectItems.addAll(toSelectItems(userRepo.findByFriendReferralId(referralUserId, UserCoreInfo.class), withAll, null));

        return selectItems;
    }

    public List<SelectItemDTO> getActiveUsersByRoles(Set<String> roles) {
        return toSelectItems(userRepo.findByRoleInAndEnabled(roles, true), false, null);
    }

    public List<SelectItemDTO> getActiveUsersForSelectByListAndRoles(Set<String> list, Set<String> role) {
        return toSelectItems(userRepo.findByIdInAndRoleInAndEnabled(list, role, true), false, null);
    }

    public List<SelectItemDTO> getActiveUsersForSelectByBlackListAndRoles(Set<String> blackList, Set<String> role) {
        return toSelectItems(userRepo.findByIdNotInAndRoleInAndEnabled(blackList, role, true), false, null);
    }

    private List<SelectItemDTO> toSelectItems(List<UserCoreInfo> users, boolean withAll, String allLabel) {
        Stream<UserCoreInfo> userStream = users.stream()
                .sorted(getUserSorting());

        String emptySelectionLabel = Optional.ofNullable(allLabel).orElse("Tutti");

        return ListConverter.fromStream(userStream)
                .toSelectItems(this::getSelectItem, withAll, emptySelectionLabel);
    }

    public Map<String, String> getUsersFullNameMap(Set<String> userIds) {
        return userRepo.findByIdIn(userIds, UserCoreInfo.class).stream()
                .collect(Collectors.toMap(UserCoreInfo::getId, this::getUserDisplayName));
    }


    private SelectItemDTO getSelectItem(UserCoreInfo user) {
        String icon = user.isEnabled() ? "" : DISABLED_ICON;
        String label = icon + getUserDisplayName(user);

        return new SelectItemDTO(user.getId(), label);
    }

    private String getUserDisplayName(UserCoreInfo user) {
        return getUserDisplayName(user.getFirstName(), user.getLastName());
    }

    public String getUserDisplayName(User user) {
        return getUserDisplayName(user.getFirstName(), user.getLastName());
    }

    public String getUserDisplayName(String firstName, String lastName) {
        if (configurationService.getUserSorting() == ConfigurationService.UserSorting.SurnameFirst)
            return lastName + " " + firstName;

        return firstName + " " + lastName;
    }

    public Comparator<UserCoreInfo> getUserSorting() {
        if (configurationService.getUserSorting() == ConfigurationService.UserSorting.SurnameFirst)
            return Comparator
                    .comparing(UserCoreInfo::getLastName)
                    .thenComparing(UserCoreInfo::getFirstName);

        return Comparator
                .comparing(UserCoreInfo::getFirstName)
                .thenComparing(UserCoreInfo::getLastName);
    }

    public List<UserDTO> getFriends(String friendReferralId) {
        return convertFromModel(userRepo.findByFriendReferralId(friendReferralId, User.class));
    }

    public List<UserDTO> getUsers(User.Role role) {
        List<User> users = role != null ? userRepo.findByRole(role.name()) : userRepo.findAll();
        return convertFromModel(users);
    }

    public List<UserDTO> getUsersByRoles(Set<String> roles) {
        List<User> users = !roles.isEmpty() ? userRepo.findByRoleIn(roles) : userRepo.findAll();
        return convertFromModel(users);
    }

    private List<UserDTO> convertFromModel(List<User> users) {
        return users.stream()
                .sorted(getUserSorting())
                .map(u -> new UserDTO().fromModel(u))
                .collect(Collectors.toList());
    }

    public Set<String> getAllUserRolesAsString(boolean includeAdmin, boolean includeFriend) {
        return Arrays.stream(User.Role.values())
                .filter(r -> includeAdmin || !r.isAdmin())
                .filter(r -> includeFriend || !r.isFriend())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    public User createFriend(UserDTO userDTO, String friendReferral) {
        userDTO.setFriendReferralId(friendReferral);
        return create(userDTO);
    }

    @Transactional
    public void resetPassword(PasswordResetDTO passwordResetDTO) throws GoGasException {
        User user = userRepo.findByUsernameAndEmail(passwordResetDTO.getUsername(), passwordResetDTO.getEmail());

        if (user == null)
            throw new ItemNotFoundException("user", passwordResetDTO.getUsername());

        resetPassword(user);
    }

    @Transactional
    public void resetPassword(String userId) throws GoGasException {
        User user = getRequired(userId);
        resetPassword(user);
    }

    private void resetPassword(User user) throws GoGasException {
        if (user.getEmail() == null)
            throw new GoGasException("L'utente non ha l'indirizzo email configurato");

        String randomPassword = RandomPassword.generate();
        String encodedPassword = passwordEncoder.encode(randomPassword);
        userRepo.updatePassword(user.getId(), encodedPassword);

        log.info("Sending mail message for password reset to user {} ({})", user.getUsername(), user.getEmail());
        mailNotificationSender.sendResetPasswordMessage(user, randomPassword);
    }

    @Transactional
    public void changePassword(String userId, PasswordChangeDTO passwordChangeDTO) throws GoGasException {
        if (passwordChangeDTO.getNewPassword() == null || passwordChangeDTO.getOldPassword() == null)
            throw new MissingOrInvalidParameterException("Invalid password");

        User user = getRequired(userId);

        String encodedOldPassword = passwordEncoder.encode(passwordChangeDTO.getOldPassword());
        if (!user.getPassword().equalsIgnoreCase(encodedOldPassword))
            throw new MissingOrInvalidParameterException("Wrong password");

        String encodedPassword = passwordEncoder.encode(passwordChangeDTO.getNewPassword());
        userRepo.updatePassword(user.getId(), encodedPassword);

        log.info("Password changed for user {}", user.getUsername());
    }

    public boolean userAlreadyExists(String username) {
        return userRepo.findByUsername(username).isPresent();
    }

    @Transactional
    public void storePushToken(String userId, PushTokenDTO pushTokenDTO) {
        PushToken pushToken = new PushToken();
        pushToken.setUserId(userId);
        pushToken.setToken(pushTokenDTO.getToken());
        pushToken.setDeviceId(pushTokenDTO.getDeviceId());

        pushTokenRepo.save(pushToken);
    }

    @Transactional
    public void deletePushToken(String userId, PushTokenDTO pushTokenDTO) {
        int deletedRows = pushTokenRepo.deleteByUserIdAndToken(userId, pushTokenDTO.getToken());

        if (deletedRows <= 0) {
            throw new ItemNotFoundException("Push token", pushTokenDTO.getToken());
        }
    }
}
