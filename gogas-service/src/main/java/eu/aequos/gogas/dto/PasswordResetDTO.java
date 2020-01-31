package eu.aequos.gogas.dto;

import lombok.Data;

@Data
public class PasswordResetDTO {
    private String username;
    private String email;
}
