package eu.aequos.gogas.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(value = "spring.datasource")
public class MasterDatasetConfig {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}