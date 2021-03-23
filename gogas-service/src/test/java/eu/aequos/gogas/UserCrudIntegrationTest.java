package eu.aequos.gogas;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.mvc.MockMvcGoGas;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.UserRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserCrudIntegrationTest {

    @Autowired
    private MockMvcGoGas mockMvcGoGas;

    @Autowired
    private UserRepo userRepo;

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
        mockMvcGoGas.executeOnRepo(() -> {
            userRepo.findByUsername("test-admin")
                    .ifPresent(userRepo::delete);
            return null;
        });
    }

    @Test
    void givenAnInvalidRole_whenCreating_anErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        validUserDTO.setRole("Y");

        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().is4xxClientError());

        Optional<User> userEntityOpt = mockMvcGoGas.executeOnRepo(() -> userRepo.findByUsername("test-admin"));
        assertFalse(userEntityOpt.isPresent());
    }

    @Test
    void givenAnEmptyUserDefinition_whenCreating_anErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test-admin");

        mockMvcGoGas.post("/api/user", userDTO)
                .andExpect(status().is4xxClientError());

        Optional<User> userEntityOpt = mockMvcGoGas.executeOnRepo(() -> userRepo.findByUsername("test-admin"));
        assertFalse(userEntityOpt.isPresent());
    }

    @Test
    void givenAValidUserDefinition_whenCreating_theUserIsCreated() throws Exception {
        mockMvcGoGas.loginAsAdmin();
        mockMvcGoGas.post("/api/user", validUserDTO)
                .andExpect(status().isOk());

        Optional<User> userEntityOpt = mockMvcGoGas.executeOnRepo(() -> userRepo.findByUsername("test-admin"));
        assertTrue(userEntityOpt.isPresent());

        User userEntity = userEntityOpt.get();
        assertEquals("test-admin", userEntity.getUsername());
        assertNotNull(userEntity.getPassword());
        assertTrue(userEntity.isEnabled());
        assertEquals("A", userEntity.getRole());
        assertEquals("Pinco", userEntity.getFirstName());
        assertEquals("Pallino", userEntity.getLastName());
        assertNull(userEntity.getFriendReferral());
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

        validUserDTO.setFirstName("Pinco2");
        validUserDTO.setLastName("Pallino2");

        mockMvcGoGas.put("/api/user/" + userIdResponse.getData(), validUserDTO)
                .andExpect(status().isOk());

        User userEntity = mockMvcGoGas.executeOnRepo(() -> userRepo.findByUsername("test-admin")).get();
        assertEquals("Pinco2", userEntity.getFirstName());
        assertEquals("Pallino2", userEntity.getLastName());
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

        Optional<User> userEntityOpt = mockMvcGoGas.executeOnRepo(() -> userRepo.findByUsername("test-admin"));
        assertFalse(userEntityOpt.isPresent());
    }

    //TODO: list with filter on role, cannot delete for constraints, friend, password reset, password change, validation cross fields (e.g. friend+referralId)
}
