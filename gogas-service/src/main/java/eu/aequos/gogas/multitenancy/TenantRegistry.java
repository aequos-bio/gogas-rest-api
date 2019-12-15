package eu.aequos.gogas.multitenancy;

import eu.aequos.gogas.configuration.MasterDatasetConfig;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TenantRegistry {

    private static final String SQLSERVER_DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String TENANTS_QUERY = "SELECT tenant_id, username, password, url FROM tenants";
    private static final String FLYWAY_BASELINE_VERSION = "20191210_001";

    private DriverManagerDataSource masterDataSource;
    private Map<Object, Object> tenantDataSourceMap;

    public TenantRegistry(MasterDatasetConfig masterConfig) {
        masterDataSource = createMasterDataSource(masterConfig);
        tenantDataSourceMap = createDataSourceMap();
    }

    public Map<Object, Object> getDataSourceMap() {
        return tenantDataSourceMap;
    }

    public boolean isValidTenant(String tenantId) {
        return tenantId != null && tenantDataSourceMap.containsKey(tenantId);
    }

    private DriverManagerDataSource createMasterDataSource(MasterDatasetConfig cfg) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(cfg.getDriverClassName());
        dataSource.setUrl(cfg.getUrl());
        dataSource.setUsername(cfg.getUsername());
        dataSource.setPassword(cfg.getPassword());

        return dataSource;
    }

    private Map<Object, Object> createDataSourceMap() {
        Map<Object, Object> dataSourceMap = new HashMap<>();

        try (
            Connection conn = masterDataSource.getConnection();
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(TENANTS_QUERY)
        ) {
            while (rs.next()) {
                String tenantId = rs.getString("tenant_id");
                DataSource tenantDataSource = createTenantDataSource(rs);

                log.info("Migrating tenant {}", tenantId);
                migrate(tenantId, tenantDataSource);

                dataSourceMap.put(tenantId, tenantDataSource);
                log.info("### Added datasource for tenant key {}");
            }
        } catch (Exception ex) {
            log.error("Error while creating tenants data source map", ex);
        }

        return dataSourceMap;
    }

    private DriverManagerDataSource createTenantDataSource(ResultSet rs) throws SQLException {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(SQLSERVER_DRIVER_CLASS);
        ds.setUrl(rs.getString("url"));
        ds.setUsername(rs.getString("username"));
        ds.setPassword(rs.getString("password"));

        return ds;
    }

    private void migrate(String tenantId, DataSource dataSource) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tenant_id", tenantId);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion(FLYWAY_BASELINE_VERSION)
                .placeholders(placeholders)
                .placeholderPrefix("$$$-bogus-$$$")
                .load();

        flyway.repair();
        flyway.migrate();
    }

    /**
     * Extract tenantID from the host name by taking the most internal subdomain
     * Example: gasname.aequos.eu -> gasname
     * @param hostName the host name in the request
     * @return the tenant id
     */
    public String extractFromHostName(String hostName) {
        if (hostName == null)
            return null;

        return hostName.split("\\.")[0];
    }
}