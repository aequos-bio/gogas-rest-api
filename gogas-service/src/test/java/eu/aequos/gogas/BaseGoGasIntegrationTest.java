package eu.aequos.gogas;

import eu.aequos.gogas.mock.MockDataLifeCycle;
import eu.aequos.gogas.mock.MockOrdersData;
import eu.aequos.gogas.mock.MockUsersData;
import eu.aequos.gogas.mvc.MockMvcGoGas;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@Slf4j
@SuppressWarnings("squid:S2187")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public class BaseGoGasIntegrationTest {

    @Autowired
    protected MockMvcGoGas mockMvcGoGas;

    @Autowired
    protected MockUsersData mockUsersData;

    @Autowired
    protected MockOrdersData mockOrdersData;

    @BeforeAll
    void init() {
        Stream.of(mockUsersData).forEach(this::initMockData);
    }

    private void initMockData(MockDataLifeCycle mockData) {
        try {
            mockData.init();
        } catch (Exception ex) {
            log.error("Error while initializing test data", ex);
        }
    }

    @AfterEach
    void clearSession() {
        mockMvcGoGas.clearUserSession();
    }

    @AfterAll
    void destroy() {
        Stream.of(mockOrdersData, mockUsersData).forEach(this::destroyMockData);
    }

    private void destroyMockData(MockDataLifeCycle mock) {
        try {
            mock.destroy();
        } catch (Exception ex) {
            log.error("Error while destroying test data", ex);
        }
    }
}
