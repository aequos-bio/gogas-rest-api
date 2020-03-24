package eu.aequos.gogas.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
public class PasswordResetDTO {
    @NotEmpty
    private String username;

    @NotEmpty
    @Email
    private String email;
}
