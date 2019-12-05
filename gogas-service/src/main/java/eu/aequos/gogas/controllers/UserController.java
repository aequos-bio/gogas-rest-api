package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsAdminOrCurrentUser;
import eu.aequos.gogas.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    public String create(@RequestBody UserDTO userDTO) {
        return userService.create(userDTO).getId();
    }

    @PutMapping(value = "{userId}")
    @IsAdmin
    public String update(@PathVariable String userId, @RequestBody UserDTO userDTO) throws ItemNotFoundException {
        return userService.update(userId, userDTO).getId();
    }

    @DeleteMapping(value = "{userId}")
    @IsAdmin
    public void delete(@PathVariable String userId) {
        userService.delete(userId);
    }
}
