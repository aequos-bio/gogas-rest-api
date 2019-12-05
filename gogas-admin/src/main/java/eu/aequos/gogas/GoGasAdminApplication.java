package eu.aequos.gogas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class GoGasAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoGasAdminApplication.class, args);
    }
}
