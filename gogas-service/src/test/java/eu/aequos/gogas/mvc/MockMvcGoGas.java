package eu.aequos.gogas.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.function.Supplier;

@Component
public class MockMvcGoGas {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Cookie jwtTokenCookie;

    @Value("${test.tenant-id:integration-test}")
    private String tenantId;

    public MockMvcGoGas(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public MockMvcGoGas loginAsAdmin() throws Exception {
        return loginAs("admin", "integration-test");
    }

    public MockMvcGoGas loginAsSimpleUser() throws Exception {
        return loginAs("user1", "user1");
    }

    public MockMvcGoGas loginAs(String username, String password) throws Exception {
        CredentialsDTO credentialsDTO = new CredentialsDTO();
        credentialsDTO.setUsername(username);
        credentialsDTO.setPassword(password);

        MvcResult authResult = mockMvc.perform(MockMvcRequestBuilders.post("/authenticate")
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(credentialsDTO)))
                .andReturn();

        jwtTokenCookie = authResult.getResponse().getCookie("jwt-token");

        return this;
    }


    public ResultActions get(String endpoint) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(endpoint)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie));
    }

    public ResultActions post(String endpoint, Object dto) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(endpoint)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie)
                .content(objectMapper.writeValueAsString(dto)));
    }

    public ResultActions put(String endpoint, Object dto) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(endpoint)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie)
                .content(objectMapper.writeValueAsString(dto)));
    }


    public ResultActions delete(String endpoint) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(endpoint)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie));
    }

    public <T> T executeOnRepo(Supplier<T> s) {
        try {
            TenantContext.setTenantId(tenantId);
            return s.get();
        } finally {
            TenantContext.clearTenantId();
        }
    }

    public <T> T parseJSON(String content, Class<T> valueType) throws IOException {
        return objectMapper.readValue(content, valueType);
    }
}
