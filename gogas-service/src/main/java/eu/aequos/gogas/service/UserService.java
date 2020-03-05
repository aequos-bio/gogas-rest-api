package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
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

    //TODO: cache users

    public UserService(ConfigurationService configurationService, UserRepo userRepo,
                       PasswordEncoder passwordEncoder) {
        super(userRepo, "user");

        this.configurationService = configurationService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SelectItemDTO> getUsersForSelect(String role, boolean withAll) {
        return toSelectItems(userRepo.findByRole(role, UserCoreInfo.class), withAll);
    }

    public User create(UserDTO dto) throws GoGasException {
        if (userRepo.findByUsername(dto.getUsername()).isPresent())
            throw new GoGasException("Esiste già un utente con la username specificata");

        return super.create(dto);
    }

    public List<SelectItemDTO> getFriendsForSelect(String referralUserId, boolean withAll, boolean includeReferral) {
        List<SelectItemDTO> selectItems = new ArrayList<>();

        if (includeReferral)
            selectItems.add(getSelectItem(getRequired(referralUserId)));

        selectItems.addAll(toSelectItems(userRepo.findByFriendReferralId(referralUserId, UserCoreInfo.class), withAll));

        return selectItems;
    }

    public List<SelectItemDTO> getActiveUsersByRoles(Set<String> roles) {
        return toSelectItems(userRepo.findByRoleInAndEnabled(roles, true), false);
    }

    public List<SelectItemDTO> getActiveUsersForSelectByBlackListAndRoles(Set<String> blackList, Set<String> role) {
        return toSelectItems(userRepo.findByIdNotInAndRoleInAndEnabled(blackList, role, true), false);
    }

    private List<SelectItemDTO> toSelectItems(List<UserCoreInfo> users, boolean withAll) {
        Stream<UserCoreInfo> userStream = users.stream()
                .sorted(getUserSorting());

        return ListConverter.fromStream(userStream)
                .toSelectItems(this::getSelectItem, withAll, "Tutti");
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

    public List<UserDTO> getUsers(String role) {
        List<User> users = role != null ? userRepo.findByRole(role) : userRepo.findAll();
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
        return super.create(userDTO);
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

        log.info("New password is {}", randomPassword);
        //TODO: send email
    }

    @Transactional
    public void changePassword(String userId, PasswordChangeDTO passwordChangeDTO) throws GoGasException {
        if (passwordChangeDTO.getNewPassword() == null || passwordChangeDTO.getOldPassword() == null)
            throw new GoGasException("Invalid password");

        User user = getRequired(userId);

        String encodedOldPassword = passwordEncoder.encode(passwordChangeDTO.getOldPassword());
        if (!user.getPassword().equalsIgnoreCase(encodedOldPassword))
            throw new GoGasException("Wrong password");

        String encodedPassword = passwordEncoder.encode(passwordChangeDTO.getNewPassword());
        userRepo.updatePassword(user.getId(), encodedPassword);

        log.info("New password is {}", passwordChangeDTO.getNewPassword());
    }
}
