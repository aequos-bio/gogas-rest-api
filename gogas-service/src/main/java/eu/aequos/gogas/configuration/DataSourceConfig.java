package eu.aequos.gogas.configuration;

import eu.aequos.gogas.datasource.CustomRoutingDataSource;
import eu.aequos.gogas.datasource.DataSourceRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Autowired
    private MasterDatasetConfig masterConfig;

    @Bean
    public DataSource dataSource() throws Exception {
        CustomRoutingDataSource customDataSource = new CustomRoutingDataSource();
        customDataSource.setTargetDataSources(DataSourceRegistry.getDataSourceMap(masterConfig));
        return customDataSource;
    }
}
