package eu.aequos.gogas.security;

import lombok.Data;

@Data
public class JwtAuthenticationRequest {
    private String username;
    private String password;
}
