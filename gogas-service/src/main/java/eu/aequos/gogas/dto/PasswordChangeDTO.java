package eu.aequos.gogas.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class PasswordChangeDTO {
    @NotEmpty
    private String oldPassword;

    @NotEmpty
    private String newPassword;
}
