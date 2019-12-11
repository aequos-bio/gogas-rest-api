package eu.aequos.gogas.configuration;

import eu.aequos.gogas.datasource.CustomRoutingDataSource;
import eu.aequos.gogas.datasource.DataSourceRegistry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);
    private static final String FLYWAY_BASELINE_VERSION = "20191210_001";

    @Autowired
    private MasterDatasetConfig masterConfig;

    @Bean
    public DataSource dataSource() throws Exception {
        Map<Object, Object> dataSourcesMap = DataSourceRegistry.getDataSourceMap(masterConfig);
        migrate(dataSourcesMap);

        CustomRoutingDataSource customDataSource = new CustomRoutingDataSource();
        customDataSource.setTargetDataSources(dataSourcesMap);
        return customDataSource;
    }

    private void migrate(Map<Object, Object> dataSourcesMap) {
        for (Map.Entry<Object, Object> entry : dataSourcesMap.entrySet()) {
            DataSource dataSource = (DataSource) entry.getValue();

            LOGGER.info("Migrating tenant {}", entry.getKey());

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("tenant_id", entry.getKey().toString());

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
    }
}
