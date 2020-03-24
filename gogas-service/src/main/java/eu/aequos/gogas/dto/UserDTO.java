package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.User;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.Optional;

@Data
public final class UserDTO implements ConvertibleDTO<User> {

    @JsonProperty(value = "idUtente")
    private String id;

    @NotEmpty
    @JsonProperty(value = "username")
    private String username;

    @NotEmpty
    @JsonProperty(value = "password")
    private String password;

    @JsonIgnore
    private String hashedPassword;

    @NotEmpty
    @Pattern(regexp = "A|U|S", message = "wrong role type")
    @JsonProperty(value = "ruolo")
    private String role;

    @JsonProperty(value = "ruololabel")
    private String roleLabel;

    @NotEmpty
    @JsonProperty(value = "nome")
    private String firstName;

    @NotEmpty
    @JsonProperty(value = "cognome")
    private String lastName;

    @Email
    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "attivo")
    private boolean enabled;

    @JsonProperty(value = "telefono")
    private String phone;

    @JsonProperty(value = "idReferente")
    private String friendReferralId;

    @JsonProperty(value = "nomeReferente")
    private String friendReferralName;

    @Override
    public UserDTO fromModel(User user) {
        id = user.getId();
        username = user.getUsername();
        role = user.getRole();
        roleLabel = user.getRoleEnum().getLabel();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        email = user.getEmail();
        phone = user.getPhone();
        enabled = user.isEnabled();

        User friendReferral = user.getFriendReferral();
        if (friendReferral != null) {
            friendReferralId = friendReferral.getId();
            friendReferralName = friendReferral.getFirstName() + " " + friendReferral.getLastName();
        }

        return this;
    }

    @Override
    public User toModel(Optional<User> existingUser) {
        User model = existingUser.orElse(new User());

        model.setUsername(username);
        model.setRole(role);
        model.setFirstName(firstName);
        model.setLastName(lastName);
        model.setEmail(email);
        model.setPhone(phone);
        model.setEnabled(enabled);

        if (hashedPassword != null && !hashedPassword.isEmpty())
            model.setPassword(hashedPassword);

        if (friendReferralId != null && !friendReferralId.isEmpty())
            model.setFriendReferral(new User().withUserId(friendReferralId));

        return model;
    }
}
