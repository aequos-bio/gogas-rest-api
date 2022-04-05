package eu.aequos.gogas.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.dto.CredentialsDTO;
import eu.aequos.gogas.mock.MockUsersData;
import eu.aequos.gogas.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        return loginAs(MockUsersData.SIMPLE_USER_USERNAME, MockUsersData.SIMPLE_USER_PASSWORD);
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

        MockHttpServletResponse response = authResult.getResponse();

        if (response.getStatus() != 200) {
            throw new Exception(String.format("Error while login with user %s. Response status: %s", username, response.getStatus()));
        }

        jwtTokenCookie = response.getCookie("jwt-token");

        return this;
    }

    public void clearUserSession() {
        jwtTokenCookie = null;
    }

    public ResultActions get(String endpoint) throws Exception {
        return get(endpoint, new LinkedMultiValueMap<>());
    }

    public ResultActions get(String endpoint, MultiValueMap<String, String> params) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(endpoint)
                .params(params)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie));
    }

    public <T> T getDTO(String endpoint, Class<T> dtoClass) throws Exception {
        return extractDTO(get(endpoint, new LinkedMultiValueMap<>()), dtoClass);
    }

    public <T> List<T> getDTOList(String endpoint, Class<T> dtoClass) throws Exception {
        return extractListDTO(get(endpoint, new LinkedMultiValueMap<>()), dtoClass);
    }

    public <T> List<T> getDTOList(String endpoint, Class<T> dtoClass, Map<String, List<String>> requestParams) throws Exception {
        return extractListDTO(get(endpoint, new LinkedMultiValueMap<>(requestParams)), dtoClass);
    }

    public ResultActions post(String endpoint, Object dto) throws Exception {
        return post(endpoint, dto, new LinkedMultiValueMap<>());
    }

    public ResultActions post(String endpoint, Object dto, MultiValueMap<String, String> params) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(endpoint)
                .params(params)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie)
                .content(objectMapper.writeValueAsString(dto)));
    }

    public ResultActions post(String endpoint, InputStream contentStream, String originalFileName, String contentType) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                .file(new MockMultipartFile("file", "", contentType, contentStream))
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType(contentType)
                .cookie(jwtTokenCookie));
    }

    public <T> T postDTO(String endpoint, Object dto, Class<T> dtoClass) throws Exception {
        ResultActions callResult = post(endpoint, dto);
        return extractDTO(callResult, dtoClass);
    }

    public <T> T postDTO(String endpoint, InputStream contentStream, String originalFileName, String contentType, Class<T> dtoClass) throws Exception {
        ResultActions callResult = post(endpoint, contentStream, originalFileName, contentType);
        return extractDTO(callResult, dtoClass);
    }

    public <T> T postDTO(String endpoint, Object dto, Class<T> dtoClass, Map<String, List<String>> requestParams) throws Exception {
        ResultActions callResult = post(endpoint, dto, new LinkedMultiValueMap<>(requestParams));
        return extractDTO(callResult, dtoClass);
    }

    public <T> List<T> postDTOList(String endpoint, Object dto, Class<T> dtoClass) throws Exception {
        ResultActions callResult = post(endpoint, dto);
        return extractListDTO(callResult, dtoClass);
    }

    public ResultActions put(String endpoint) throws Exception {
        return put(endpoint, null);
    }

    public <T> T putDTO(String endpoint, Class<T> dtoClass) throws Exception {
        ResultActions callResult = put(endpoint, null);
        return extractDTO(callResult, dtoClass);
    }

    public <T> T putDTO(String endpoint, Object dto, Class<T> dtoClass) throws Exception {
        ResultActions callResult = put(endpoint, dto);
        return extractDTO(callResult, dtoClass);
    }

    public ResultActions put(String endpoint, Object dto) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(endpoint)
                .with(req -> {
                    req.setServerName(tenantId + ".aequos.bio");
                    return req;
                })
                .contentType("application/json")
                .cookie(jwtTokenCookie);

        if (dto != null) {
            requestBuilder.content(serializeDTO(dto));
        }

        return mockMvc.perform(requestBuilder);
    }

    private String serializeDTO(Object dto) throws JsonProcessingException {
        if (dto instanceof String)
            return dto.toString();

        return objectMapper.writeValueAsString(dto);
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

    public <T> T deleteDTO(String endpoint, Class<T> dtoClass) throws Exception {
        return extractDTO(delete(endpoint), dtoClass);
    }

    public <T> T executeOnRepo(Supplier<T> s) {
        try {
            TenantContext.setTenantId(tenantId);
            return s.get();
        } finally {
            TenantContext.clearTenantId();
        }
    }

    private <T> T extractDTO(ResultActions callResult, Class<T> dtoClass) throws Exception {
        String responseBody = callResult
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, dtoClass);
    }

    private <T> List<T> extractListDTO(ResultActions callResult, Class<T> dtoClass) throws Exception {
        String responseBody = callResult
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseBody, objectMapper.getTypeFactory().constructCollectionType(List.class, dtoClass));
    }
}
