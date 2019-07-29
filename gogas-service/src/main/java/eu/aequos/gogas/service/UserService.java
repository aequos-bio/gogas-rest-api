package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.SelectItemsConverter;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.persistence.entity.derived.UserCoreInfo;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService extends CrudService<User, String> {

    private final String DISABLED_ICON = "<span class='glyphicon glyphicon-ban-circle' style='margin-right:10px'></span>";

    private ConfigurationService configurationService;
    private SelectItemsConverter selectItemsConverter;
    private UserRepo userRepo;

    public UserService(ConfigurationService configurationService, SelectItemsConverter selectItemsConverter, UserRepo userRepo) {
        super(userRepo, "user");

        this.configurationService = configurationService;
        this.selectItemsConverter = selectItemsConverter;
        this.userRepo = userRepo;
    }

    public List<SelectItemDTO> getUsersForSelect(String role, boolean withAll) {
        Stream<UserCoreInfo> users = userRepo.findByRole(role, UserCoreInfo.class).stream()
                .sorted(getUserSorting());

        return selectItemsConverter.toSelectItems(users, this::getSelectItem, withAll, "Tutti");
    }

    private SelectItemDTO getSelectItem(UserCoreInfo user) {
        String icon = user.isEnabled() ? "" : DISABLED_ICON;
        String label = icon + getUserDisplayName(user.getFirstName(), user.getLastName());

        return new SelectItemDTO(user.getId(), label);
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

    public List<UserDTO> getUsers(String role) {
        List<User> users = role != null ? userRepo.findByRole(role) : userRepo.findAll();

        return users.stream()
                .sorted(getUserSorting())
                .map(u -> new UserDTO().fromModel(u))
                .collect(Collectors.toList());
    }
}
