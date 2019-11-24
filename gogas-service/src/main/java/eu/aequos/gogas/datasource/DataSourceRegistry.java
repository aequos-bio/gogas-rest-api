package eu.aequos.gogas.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import eu.aequos.gogas.configuration.MasterDatasetConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DataSourceRegistry {
    public static Map<Object, Object> getDataSourceMap(MasterDatasetConfig cfg) throws SQLException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(cfg.getDriverClassName());
        dataSource.setUrl(cfg.getUrl());
        dataSource.setUsername(cfg.getUsername());
        dataSource.setPassword(cfg.getPassword());

        Connection conn = dataSource.getConnection();
        Statement stat = conn.createStatement();
        String sql = "select tenant_id, username, password, url from tenants";
        ResultSet rs = stat.executeQuery(sql);
        Map<Object, Object> hashMap = new HashMap<>();
        while(rs.next()) {
            String tenantId = rs.getString("tenant_id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String url = rs.getString("url");
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            hashMap.put(tenantId, ds);    
            System.out.println("### Addes datasource for tenant key " + tenantId);
        }

        return hashMap;
    }

}
