package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("api/user")
public class UserController {

    private UserService userService;
    private AuthorizationService authorizationService;

    public UserController(UserService userService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdmin
    public List<SelectItemDTO> listUsersForSelect(@RequestParam String role,
                                                  @RequestParam(required = false) boolean withAll) {
        return userService.getUsersForSelect(role, withAll);
    }

    @GetMapping(value = "{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdminOrCurrentUser
    public UserDTO getUser(@PathVariable String userId) {
        return new UserDTO().fromModel(userService.getRequired(userId));
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdmin
    public List<UserDTO> listUsers(@RequestParam(required = false) String role) {
        return userService.getUsers(role);
    }

    @PostMapping()
    @IsAdmin
    public BasicResponseDTO create(@Valid @RequestBody UserDTO userDTO) throws GoGasException {
        String userId = userService.create(userDTO).getId();
        return new BasicResponseDTO(userId);
    }

    @PutMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO update(@PathVariable String userId, @Valid @RequestBody UserDTO userDTO) throws ItemNotFoundException {
        String updatedUserId = userService.update(userId, userDTO).getId();
        return new BasicResponseDTO(updatedUserId);
    }

    @DeleteMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO delete(@PathVariable String userId) {
        userService.delete(userId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{userId}/password/reset")
    @IsAdmin
    public BasicResponseDTO resetPassword(@PathVariable String userId) throws GoGasException {
        userService.resetPassword(userId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "password/reset")
    public BasicResponseDTO resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) throws GoGasException {
        userService.resetPassword(passwordResetDTO);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "password/change")
    public BasicResponseDTO changePassword(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO) throws GoGasException {
        GoGasUserDetails currentUser = authorizationService.getCurrentUser();
        userService.changePassword(currentUser.getId(), passwordChangeDTO);
        return new BasicResponseDTO("OK");
    }
}
