package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsCurrentUserFriend;
import eu.aequos.gogas.service.AccountingService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/friend")
public class FriendController {

    private UserService userService;
    private AuthorizationService authorizationService;
    private AccountingService accountingService;
    private ConfigurationService configurationService;

    public FriendController(UserService userService, AuthorizationService authorizationService,
                            AccountingService accountingService, ConfigurationService configurationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
        this.accountingService = accountingService;
        this.configurationService = configurationService;
    }

    @GetMapping(value = "select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<SelectItemDTO> listFriendsForSelect(@RequestParam(required = false) boolean withAll,
                                                    @RequestParam(required = false) boolean includeReferral) {

        return userService.getFriendsForSelect(authorizationService.getCurrentUser().getId(), withAll, includeReferral);
    }

    @IsCurrentUserFriend
    @GetMapping(value = "{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public UserDTO getFriend(@PathVariable String userId) {
        return new UserDTO().fromModel(userService.getRequired(userId));
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserDTO> listFriends() {
        return userService.getFriends(authorizationService.getCurrentUser().getId());
    }

    @PostMapping()
    public String create(@RequestBody UserDTO userDTO) {
        return userService.createFriend(userDTO, authorizationService.getCurrentUser().getId())
                .getId();
    }

    @IsCurrentUserFriend
    @PutMapping(value = "{userId}")
    public String update(@PathVariable String userId, @RequestBody UserDTO userDTO) throws ItemNotFoundException {
        return userService.update(userId, userDTO)
                .getId();
    }

    @IsCurrentUserFriend
    @DeleteMapping(value = "{userId}")
    public void delete(@PathVariable String userId) {
        userService.delete(userId);
    }
}
