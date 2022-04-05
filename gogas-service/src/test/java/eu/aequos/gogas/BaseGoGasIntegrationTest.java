package eu.aequos.gogas;

import eu.aequos.gogas.attachments.AttachmentRepo;
import eu.aequos.gogas.mock.MockDataLifeCycle;
import eu.aequos.gogas.mock.MockOrdersData;
import eu.aequos.gogas.mock.MockUsersData;
import eu.aequos.gogas.mvc.MockMvcGoGas;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@Slf4j
@SuppressWarnings("squid:S2187")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public class BaseGoGasIntegrationTest {

    @TempDir
    protected Path repoFolder;

    @MockBean
    private AttachmentRepo attachmentRepo;

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

    @BeforeEach
    void mockRepo() {
        when(attachmentRepo.getRootFolder()).thenReturn(repoFolder.toFile().getAbsolutePath());
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
