package eu.aequos.gogas.configuration;

import eu.aequos.gogas.datasource.CustomRoutingDataSource;
import eu.aequos.gogas.datasource.DataSourceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(){
        CustomRoutingDataSource customDataSource = new CustomRoutingDataSource();
        customDataSource.setTargetDataSources(DataSourceRegistry.getDataSourceMap());
        return customDataSource;
    }
}
