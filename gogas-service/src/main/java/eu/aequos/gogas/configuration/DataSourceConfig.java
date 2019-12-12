package eu.aequos.gogas.configuration;

import eu.aequos.gogas.multitenancy.TenantRegistry;
import eu.aequos.gogas.multitenancy.TenantRoutingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private TenantRegistry tenantRegistry;

    public DataSourceConfig(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    @Bean
    public DataSource dataSource() {
        return new TenantRoutingDataSource(tenantRegistry);
    }
}
