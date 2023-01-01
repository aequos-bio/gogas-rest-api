package eu.aequos.gogas;

import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

public class GoGasDatabaseContainer extends MSSQLServerContainer<GoGasDatabaseContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mcr.microsoft.com/mssql/server");
    private static final String IMAGE_VERSION = "2022-latest";

    private static GoGasDatabaseContainer container;

    static {
        try {
            container = new GoGasDatabaseContainer().acceptLicense();
            container.withPassword("Int3grat1on_te5T!");
            container.withInitScript("eu/aequos/gogas/init.sql");
            container.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private GoGasDatabaseContainer() {
        super(DEFAULT_IMAGE_NAME.withTag(IMAGE_VERSION));
    }

    public static GoGasDatabaseContainer getInstance() {
        return container;
    }

    @Override
    public void start() {
        super.start();

        /*System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());*/

        Properties connectionProps = new Properties();
        connectionProps.put("user", getUsername());
        connectionProps.put("password", getPassword());

        try (Connection connect = getJdbcDriverInstance().connect(getJdbcUrl(), connectionProps)) {

            PreparedStatement preparedStatement = connect.prepareStatement("" +
                    "INSERT INTO tenants (tenant_id, username, [password], [url])\n" +
                    "VALUES ('localhost', 'sa', 'Int3grat1on_te5T!', '" + getJdbcUrl() + ";database=gogas_integration_test');\n");
            preparedStatement.execute();

            PreparedStatement preparedStatement2 = connect.prepareStatement("" +
                    "INSERT INTO tenants (tenant_id, username, [password], [url])\n" +
                    "VALUES ('integration-test', 'sa', 'Int3grat1on_te5T!', '" + getJdbcUrl() + ";database=gogas_integration_test');\n");
            preparedStatement2.execute();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
