package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.SelectItemsConverter;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

@Service
public class UserService extends CrudService<User, String> {

    private final String DISABLED_ICON = "<span class='glyphicon glyphicon-ban-circle' style='margin-right:10px'></span>";

    private ConfigurationService configurationService;
    private SelectItemsConverter selectItemsConverter;
    private UserRepo userRepo;

    //TODO: cache users

    public UserService(ConfigurationService configurationService, SelectItemsConverter selectItemsConverter, UserRepo userRepo) {
        super(userRepo, "user");

        this.configurationService = configurationService;
        this.selectItemsConverter = selectItemsConverter;
        this.userRepo = userRepo;
    }

    public List<SelectItemDTO> getUsersForSelect(String role, boolean withAll) {
        return toSelectItems(userRepo.findByRole(role, UserCoreInfo.class), withAll);
    }

    public List<SelectItemDTO> getFriendsForSelect(String referralUserId, boolean withAll) {
        return toSelectItems(userRepo.findByFriendReferralId(referralUserId, UserCoreInfo.class), withAll);
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

        return selectItemsConverter.toSelectItems(userStream, this::getSelectItem, false, "Tutti");
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
}
