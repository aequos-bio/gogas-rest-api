package eu.aequos.gogas.users;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserCrudIntegrationTest extends BaseGoGasIntegrationTest {

    private UserDTO validUserDTO;

    @BeforeEach
    void setUp() {
        validUserDTO = new UserDTO();
        validUserDTO.setUsername("test-admin");
        validUserDTO.setPassword("pwd");
        validUserDTO.setRole("A");
        validUserDTO.setFirstName("Pinco");
        validUserDTO.setLastName("Pallino");
        validUserDTO.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        mockUsersData.deleteByUsername("test-admin");
    }

    @Test
    void givenAnInvalidRole_whenCreating_anErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        validUserDTO.setRole("Y");

        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().isBadRequest());

        List<UserDTO> allUsers = mockMvcGoGas.getDTOList("/api/user/list", UserDTO.class);
        assertFalse(allUsers.stream().anyMatch(user -> user.getUsername().equals("test-admin")));
    }

    @Test
    void givenAnEmptyUserDefinition_whenCreating_anErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test-admin");

        mockMvcGoGas.post("/api/user", userDTO)
                .andExpect(status().isBadRequest());

        List<UserDTO> allUsers = mockMvcGoGas.getDTOList("/api/user/list", UserDTO.class);
        assertFalse(allUsers.stream().anyMatch(user -> user.getUsername().equals("test-admin")));
    }

    @Test
    void givenAValidUserDefinition_whenCreating_theUserIsCreated() throws Exception {
        mockMvcGoGas.loginAsAdmin();
        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/user", validUserDTO, BasicResponseDTO.class);
        String userId = basicResponseDTO.getData().toString();

        UserDTO user = mockMvcGoGas.getDTO("/api/user/" + userId, UserDTO.class);

        assertEquals("test-admin", user.getUsername());
        assertTrue(user.isEnabled());
        assertEquals("A", user.getRole());
        assertEquals("Pinco", user.getFirstName());
        assertEquals("Pallino", user.getLastName());
        assertNull(user.getFriendReferralId());
    }

    @Test
    void givenAValidUserDefinition_whenCreating_theUserCanLogin() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().isOk());

        mockMvcGoGas.loginAs(validUserDTO.getUsername(), validUserDTO.getPassword());
    }

    @Test
    void givenADuplicatedUsername_whenCreating_theUserIsNotCreated() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().isOk());

        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().is(409));
    }

    @Test
    void givenANonExistingUser_whenUpdating_NotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.put("/api/user/58C32B3D-C814-4086-AF3D-1154593884FF", validUserDTO)
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnExistingUser_whenUpdating_theUserIsUpdated() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO userIdResponse = mockMvcGoGas.postDTO("/api/user",  validUserDTO, BasicResponseDTO.class);
        String userId = userIdResponse.getData().toString();

        validUserDTO.setFirstName("Pinco2");
        validUserDTO.setLastName("Pallino2");

        mockMvcGoGas.put("/api/user/" + userId, validUserDTO)
                .andExpect(status().isOk());

        UserDTO user = mockMvcGoGas.getDTO("/api/user/" + userId, UserDTO.class);
        assertEquals("Pinco2", user.getFirstName());
        assertEquals("Pallino2", user.getLastName());
    }

    @Test
    void givenANonExistingUser_whenDeleting_NotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.delete("/api/user/58C32B3D-C814-4086-AF3D-1154593884FF")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnExistingUser_whenDeleting_theUserIsDeleted() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        BasicResponseDTO userIdResponse = mockMvcGoGas.postDTO("/api/user", validUserDTO, BasicResponseDTO.class);

        mockMvcGoGas.delete("/api/user/" + userIdResponse.getData())
                .andExpect(status().isOk());

        mockMvcGoGas.get("/api/user/" + userIdResponse.getData())
                .andExpect(status().isNotFound());
    }

    //TODO: list with filter on role, cannot delete for constraints, friend, password reset, password change, validation cross fields (e.g. friend+referralId)
}
