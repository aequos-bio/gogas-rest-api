package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
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
@RequestMapping("api/user")
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
    public BasicResponseDTO create(@RequestBody UserDTO userDTO) {
        String userId = userService.create(userDTO).getId();
        return new BasicResponseDTO(userId);
    }

    @PutMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO update(@PathVariable String userId, @RequestBody UserDTO userDTO) throws ItemNotFoundException {
        String updatedUserId = userService.update(userId, userDTO).getId();
        return new BasicResponseDTO(updatedUserId);
    }

    @DeleteMapping(value = "{userId}")
    @IsAdmin
    public BasicResponseDTO delete(@PathVariable String userId) {
        userService.delete(userId);
        return new BasicResponseDTO("OK");
    }
}
