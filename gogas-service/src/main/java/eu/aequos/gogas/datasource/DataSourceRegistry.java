package eu.aequos.gogas.datasource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.HashMap;
import java.util.Map;

public class DataSourceRegistry {

    public static Map<Object, Object> getDataSourceMap() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl("jdbc:sqlserver://localhost;databaseName=gastabien");
        dataSource.setUsername("gastabien");
        dataSource.setPassword("gastabien");

        DriverManagerDataSource dataSource1 = new DriverManagerDataSource();
        dataSource1.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource1.setUrl("jdbc:sqlserver://localhost;databaseName=rovellasgas");
        dataSource1.setUsername("gastabien");
        dataSource1.setPassword("gastabien");

        Map<Object, Object> hashMap = new HashMap<>();
        hashMap.put("gastabien", dataSource);
        hashMap.put("rovellasgas", dataSource1);
        return hashMap;
    }

}
