package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.UserService;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api("Users")
@RestController
@RequestMapping("api/user")
public class UserController {

    private UserService userService;
    private AuthorizationService authorizationService;

    public UserController(UserService userService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @ApiOperation(
        value = "List for dropdown selection",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @GetMapping(value = "select", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdmin
    public List<SelectItemDTO> listForSelection(@ApiParam("user role") @RequestParam User.Role role,
                                                @ApiParam("include \"all\" entry") @RequestParam(required = false) boolean withAll,
                                                @ApiParam("\"all\" label") @RequestParam String allLabel) {
        return userService.getUsersForSelect(role, withAll, allLabel);
    }

    @ApiOperation(
        value = "Get user details",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role"), @AuthorizationScope(scope ="current user", description = "user in session") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = UserDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
    })
    @GetMapping(value = "{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdminOrCurrentUser
    public UserDTO getUser(@PathVariable String userId) {
        return new UserDTO().fromModel(userService.getRequired(userId));
    }

    @ApiOperation(
        value = "List users",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdmin
    public List<UserDTO> listUsers(@RequestParam(required = false) User.Role role) {
        return userService.getUsers(role);
    }

    @ApiOperation(
        value = "Check if username exists",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @GetMapping(value = "exists", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @IsAdmin
    public BasicResponseDTO isUserExisting(@RequestParam String username) {
        return new BasicResponseDTO(userService.userAlreadyExists(username));
    }

    @ApiOperation(
        value = "Create user",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 409, message = "L'elemento non può essere creato perché già esistente")
    })
    @PostMapping()
    @IsAdmin
    public BasicResponseDTO createUser(@Valid @RequestBody UserDTO userDTO) throws GoGasException {
        String userId = userService.create(userDTO).getId();
        return new BasicResponseDTO(userId);
    }

    @ApiOperation(
        value = "Update user",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
    })
    @PutMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO updateUser(@PathVariable String userId, @Valid @RequestBody UserDTO userDTO) throws ItemNotFoundException {
        String updatedUserId = userService.update(userId, userDTO).getId();
        return new BasicResponseDTO(updatedUserId);
    }

    @ApiOperation(
        value = "Delete user",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
        @ApiResponse(code = 409, message = "L'elemento non può essere eliminato")
    })
    @DeleteMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO deleteUser(@PathVariable String userId) {
        userService.delete(userId);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Reset user password (admin)",
        notes = "Reset password of the specific user. Operation allowed only to admin users. An email containing the new password is sent to the user.",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 400, message = "Missing or invalid parameter"),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
    })
    @PutMapping(value = "{userId}/password/reset")
    @IsAdmin
    public BasicResponseDTO resetUserPassword(@PathVariable String userId) throws GoGasException {
        userService.resetPassword(userId);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Reset own password",
        notes = "Reset password for the current user. An email containing the new password is sent to the user.",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="any role", description = "any role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 400, message = "Missing or invalid parameter"),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
    })
    @PutMapping(value = "password/reset")
    public BasicResponseDTO resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) throws GoGasException {
        userService.resetPassword(passwordResetDTO);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Change own password",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="any role", description = "any role") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 400, message = "Missing or invalid parameter"),
        @ApiResponse(code = 404, message = "Item not found. Type: user, Id: <userId>"),
    })
    @PutMapping(value = "password/change")
    public BasicResponseDTO changePassword(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO) throws GoGasException {
        GoGasUserDetails currentUser = authorizationService.getCurrentUser();
        userService.changePassword(currentUser.getId(), passwordChangeDTO);
        return new BasicResponseDTO("OK");
    }
}
