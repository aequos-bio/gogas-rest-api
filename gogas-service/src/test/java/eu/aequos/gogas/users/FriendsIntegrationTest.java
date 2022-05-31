package eu.aequos.gogas.users;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.UserDTO;
import eu.aequos.gogas.persistence.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendsIntegrationTest extends BaseGoGasIntegrationTest {

    String userId1;
    String friendId1a;
    String friendId1b;
    String userId2;
    String friendId2a;
    String friendId2b;
    String userId3;

    @BeforeAll
    void createUsersAndReasons() {
        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User friend1a = mockUsersData.createFriendUser("friend1a", "password", "f1", "a", user1);
        User friend1b = mockUsersData.createFriendUser("friend1b", "password", "f1", "b", user1);

        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");
        User friend2a = mockUsersData.createFriendUser("friend2a", "password", "f2", "a", user2);
        User friend2b = mockUsersData.createFriendUser("friend2b", "password", "f2", "b", user2);

        User user3 = mockUsersData.createSimpleUser("user3", "password", "user3", "user3");

        userId1 = user1.getId().toUpperCase();
        friendId1a = friend1a.getId().toUpperCase();
        friendId1b = friend1b.getId().toUpperCase();
        userId2 = user2.getId().toUpperCase();
        friendId2a = friend2a.getId().toUpperCase();
        friendId2b = friend2b.getId().toUpperCase();
        userId3 = user3.getId().toUpperCase();
    }

    @AfterEach
    void tearDown() {
        mockUsersData.deleteByUsername("test-friend");
        mockUsersData.deleteByUsername("changedFriend");
    }

    @Test
    void givenExistingFriends_whenGettingFriendsAsOptions_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Map<String, String> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class).stream()
                .collect(Collectors.toMap(SelectItemDTO::getId, SelectItemDTO::getDescription));

        Map<String, String> expectedSelectItems = Map.of(friendId1a, "f1 a", friendId1b, "f1 b");
        assertEquals(expectedSelectItems, selectItems);

        mockMvcGoGas.loginAs("user2", "password");

        Map<String, String> selectItems2 = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class).stream()
                .collect(Collectors.toMap(SelectItemDTO::getId, SelectItemDTO::getDescription));

        Map<String, String> expectedSelectItems2 = Map.of(friendId2a, "f2 a", friendId2b, "f2 b");
        assertEquals(expectedSelectItems2, selectItems2);
    }

    @Test
    void givenNoFriends_whenGettingFriendsAsOptions_thenNothingIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        List<SelectItemDTO> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class);
        assertTrue(selectItems.isEmpty());
    }

    @Test
    void givenAllOptionEnables_whenGettingFriendsAsOptions_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("withAll", List.of("true"));
        Map<String, String> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class, requestParams).stream()
                .collect(Collectors.toMap(SelectItemDTO::getId, SelectItemDTO::getDescription));

        Map<String, String> expectedSelectItems = Map.of("", "Tutti", friendId1a, "f1 a", friendId1b, "f1 b");
        assertEquals(expectedSelectItems, selectItems);
    }

    @Test
    void givenIncludeUserOptionEnables_whenGettingFriendsAsOptions_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("includeReferral", List.of("true"));
        Map<String, String> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class, requestParams).stream()
                .collect(Collectors.toMap(SelectItemDTO::getId, SelectItemDTO::getDescription));

        Map<String, String> expectedSelectItems = Map.of(userId1, "user1 user1", friendId1a, "f1 a", friendId1b, "f1 b");
        assertEquals(expectedSelectItems, selectItems);
    }

    @Test
    void givenAllOptionsEnables_whenGettingFriendsAsOptions_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Map<String, List<String>> requestParams = Map.of("withAll", List.of("true"), "includeReferral", List.of("true"));
        Map<String, String> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class, requestParams).stream()
                .collect(Collectors.toMap(SelectItemDTO::getId, SelectItemDTO::getDescription));

        Map<String, String> expectedSelectItems = Map.of("", "Tutti", userId1, "user1 user1", friendId1a, "f1 a", friendId1b, "f1 b");
        assertEquals(expectedSelectItems, selectItems);
    }

    @Test
    void givenAValidFriend_whenGettingFriend_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        UserDTO friend = mockMvcGoGas.getDTO("/api/friend/" + friendId1a, UserDTO.class);
        verifyUserDTO(friend, friendId1a, "friend1a", "f1", "a", userId1);
    }

    @Test
    void givenAFriendOfAnotherUser_whenGettingFriend_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user2", "password");

        mockMvcGoGas.get("/api/friend/" + friendId1a)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenInvalidFriend_whenGettingFriend_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user2", "password");

        mockMvcGoGas.get("/api/friend/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenExistingFriends_whenGettingFriendsList_thenDataIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        Map<String, UserDTO> friends = mockMvcGoGas.getDTOList("/api/friend/list", UserDTO.class).stream()
                .collect(Collectors.toMap(UserDTO::getId, Function.identity()));

        verifyUserDTO(friends.get(friendId1a), friendId1a, "friend1a", "f1", "a", userId1);
        verifyUserDTO(friends.get(friendId1b), friendId1b, "friend1b", "f1", "b", userId1);

        mockMvcGoGas.loginAs("user2", "password");

        Map<String, UserDTO> friends2 = mockMvcGoGas.getDTOList("/api/friend/list", UserDTO.class).stream()
                .collect(Collectors.toMap(UserDTO::getId, Function.identity()));

        verifyUserDTO(friends2.get(friendId2a), friendId2a, "friend2a", "f2", "a", userId2);
        verifyUserDTO(friends2.get(friendId2b), friendId2b, "friend2b", "f2", "b", userId2);
    }

    @Test
    void givenNoFriends_whenGettingFriendsList_thenNothingIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        List<UserDTO> selectItems = mockMvcGoGas.getDTOList("/api/friend/list", UserDTO.class);
        assertTrue(selectItems.isEmpty());
    }

    @Test
    void givenAValidUserDefinition_whenCreating_theFriendIsCreated() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();

        BasicResponseDTO basicResponseDTO = mockMvcGoGas.postDTO("/api/friend", validUser, BasicResponseDTO.class);
        String userId = basicResponseDTO.getData().toString();

        UserDTO user = mockMvcGoGas.getDTO("/api/friend/" + userId, UserDTO.class);

        assertEquals("test-friend", user.getUsername());
        assertTrue(user.isEnabled());
        assertEquals("S", user.getRole());
        assertEquals("Test", user.getFirstName());
        assertEquals("Friend", user.getLastName());
        assertEquals(userId3, user.getFriendReferralId());
    }

    @Test
    void givenAValidUserDefinition_whenCreating_theFriendCanLogin() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();
        mockMvcGoGas.post("/api/friend", validUser)
                .andExpect(status().isOk());

        mockMvcGoGas.loginAs(validUser.getUsername(), validUser.getPassword());
    }

    @Test
    void givenADuplicatedUsername_whenCreating_theFriendIsNotCreated() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();

        mockMvcGoGas.post("/api/friend", validUser)
                .andExpect(status().isOk());

        mockMvcGoGas.post("/api/friend", validUser)
                .andExpect(status().is(409));
    }

    @Test
    void givenAnEmptyFriendDefinition_whenCreating_anErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test-friend");

        mockMvcGoGas.post("/api/friend", userDTO)
                .andExpect(status().isBadRequest());

        List<SelectItemDTO> selectItems = mockMvcGoGas.getDTOList("/api/friend/select", SelectItemDTO.class);
        assertTrue(selectItems.isEmpty());
    }

    @Test
    void givenANonExistingFriend_whenUpdating_NotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();
        mockMvcGoGas.put("/api/friend/" + UUID.randomUUID(), validUser)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnExistingFriend_whenUpdating_theUserIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();

        BasicResponseDTO userIdResponse = mockMvcGoGas.postDTO("/api/friend", validUser, BasicResponseDTO.class);
        String userId = userIdResponse.getData().toString();

        validUser.setUsername("changedFriend");
        validUser.setFirstName("Test2");
        validUser.setLastName("Friend2");

        mockMvcGoGas.put("/api/friend/" + userId, validUser)
                .andExpect(status().isOk());

        UserDTO user = mockMvcGoGas.getDTO("/api/friend/" + userId, UserDTO.class);
        assertEquals("changedFriend", user.getUsername());
        assertEquals("Test2", user.getFirstName());
        assertEquals("Friend2", user.getLastName());
    }

    @Test
    void givenANonExistingFriend_whenDeleting_NotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        mockMvcGoGas.delete("/api/friend/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAFriendOfOtherUser_whenDeleting_NotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        mockMvcGoGas.delete("/api/friend/" + friendId1a)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnExistingFriend_whenDeleting_theUserIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("user3", "password");

        UserDTO validUser = buildValidDTO();

        BasicResponseDTO userIdResponse = mockMvcGoGas.postDTO("/api/friend", validUser, BasicResponseDTO.class);

        mockMvcGoGas.delete("/api/friend/" + userIdResponse.getData())
                .andExpect(status().isOk());

        mockMvcGoGas.get("/api/friend/" + userIdResponse.getData())
                .andExpect(status().isForbidden());
    }

    private UserDTO buildValidDTO() {
        UserDTO validUserDTO = new UserDTO();
        validUserDTO.setUsername("test-friend");
        validUserDTO.setPassword("pwd");
        validUserDTO.setRole("A");
        validUserDTO.setFirstName("Test");
        validUserDTO.setLastName("Friend");
        validUserDTO.setEnabled(true);
        return validUserDTO;
    }

    private void verifyUserDTO(UserDTO friend, String id, String username, String firstName,
                               String lastName, String referralId) {

        assertEquals(id, friend.getId());
        assertEquals(username, friend.getUsername());
        assertNull(friend.getPassword());
        assertNull(friend.getHashedPassword());
        assertEquals(firstName, friend.getFirstName());
        assertEquals(lastName, friend.getLastName());
        assertEquals("S", friend.getRole());
        assertEquals("Amico", friend.getRoleLabel());
        assertEquals(referralId, friend.getFriendReferralId());
    }
}
