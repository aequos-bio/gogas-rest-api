package eu.aequos.gogas.datasource;

import eu.aequos.gogas.configuration.MasterDatasetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DataSourceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);

    private static final String SQLSERVER_DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String TENANTS_QUERY = "SELECT tenant_id, username, password, url FROM tenants";

    public static Map<Object, Object> getDataSourceMap(MasterDatasetConfig cfg) {
        DriverManagerDataSource dataSource = createMasterDataSource(cfg);
        return createDataSourceMap(dataSource);
    }

    private static DriverManagerDataSource createMasterDataSource(MasterDatasetConfig cfg) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(cfg.getDriverClassName());
        dataSource.setUrl(cfg.getUrl());
        dataSource.setUsername(cfg.getUsername());
        dataSource.setPassword(cfg.getPassword());

        return dataSource;
    }

    private static Map<Object, Object> createDataSourceMap(DriverManagerDataSource dataSource) {
        Map<Object, Object> dataSourceMap = new HashMap<>();

        try (
            Connection conn = dataSource.getConnection();
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(TENANTS_QUERY)
        ) {
            while (rs.next()) {
                String tenantId = rs.getString("tenant_id");
                dataSourceMap.put(tenantId, createTenantDataSource(rs));
                LOGGER.info("### Added datasource for tenant key " + tenantId);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while creating tenants data source map", ex);
        }

        return dataSourceMap;
    }

    private static DriverManagerDataSource createTenantDataSource(ResultSet rs) throws SQLException {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(SQLSERVER_DRIVER_CLASS);
        ds.setUrl(rs.getString("url"));
        ds.setUsername(rs.getString("username"));
        ds.setPassword(rs.getString("password"));

        return ds;
    }


}
