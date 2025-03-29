package eu.aequos.gogas.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(value = "gogas.superadmin")
public class SuperAdminConfig {
    private String username;
    private String password;
}