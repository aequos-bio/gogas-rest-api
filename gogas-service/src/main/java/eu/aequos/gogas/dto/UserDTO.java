package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.persistence.entity.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.Optional;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

@ApiModel("User")
@Data
public final class UserDTO implements ConvertibleDTO<User> {

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty(value = "idUtente")
    private String id;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty(value = "username")
    private String username;

    @JsonProperty(value = "password")
    private String password;

    @JsonIgnore
    private String hashedPassword;

    @ApiModelProperty(required = true)
    @NotEmpty
    @Pattern(regexp = "A|U|S", message = "wrong role type")
    @JsonProperty(value = "ruolo")
    private String role;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty(value = "ruololabel")
    private String roleLabel;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty(value = "nome")
    private String firstName;

    @ApiModelProperty(required = true)
    @NotEmpty
    @JsonProperty(value = "cognome")
    private String lastName;

    @Email
    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "attivo", defaultValue = "true")
    private boolean enabled;

    @JsonProperty(value = "telefono")
    private String phone;

    @ApiModelProperty(name = "idReferente", value = "Valid only with role 'friend' (S)")
    @JsonProperty(value = "idReferente")
    private String friendReferralId;

    @ApiModelProperty(accessMode = READ_ONLY)
    @JsonProperty(value = "nomeReferente")
    private String friendReferralName;

    @ApiModelProperty(accessMode = READ_ONLY)
    private int position;

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
        position = user.getPosition();

        User friendReferral = user.getFriendReferral();
        if (friendReferral != null) {
            friendReferralId = friendReferral.getId();
            friendReferralName = friendReferral.getFirstName() + " " + friendReferral.getLastName();
        }

        return this;
    }

    @Override
    public User toModel(Optional<User> existingUser) {
        User model = existingUser.orElseGet(this::initUser);

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

    private User initUser() {
        User user = new User();
        user.setPosition(position);
        return user;
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
