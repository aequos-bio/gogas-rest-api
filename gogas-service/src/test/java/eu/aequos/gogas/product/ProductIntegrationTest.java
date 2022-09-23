package eu.aequos.gogas.product;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.persistence.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends BaseGoGasIntegrationTest {
    private OrderType orderType;
    private OrderType otherOrderType;
    private Map<String, ProductCategory> categories;
    private Map<String, Supplier> suppliers;
    private List<String> createdProducts;

    @BeforeAll
    void createOrderTypeAndUsers() {
        orderType = mockOrdersData.createOrderType("Fresco Settimanale", true);
        otherOrderType = mockOrdersData.createOrderType("Other", true);

        categories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", orderType.getId(), 3, "white")),
                entry("Frutta", mockOrdersData.createCategory("Frutta", orderType.getId(), 1, "green")),
                entry("Ortaggi", mockOrdersData.createCategory("Ortaggi", orderType.getId(), 2, "red"))
        );

        suppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL", "TN")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)", "MN")),
                entry("1131", mockOrdersData.createSupplier("1131", "ABBIATE VALERIO", "BO"))
        );

        List<Product> products = List.of(
                mockOrdersData.createProduct(orderType.getId(), "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), true, false, false, "PZ", null, 1.0, 3.65, null, null, "Mensile"),

                mockOrdersData.createProduct(orderType.getId(), "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), true, false, false, "PZ", null, 1.0, 3.65, null, null, "Settimanale"),

                mockOrdersData.createProduct(orderType.getId(), "MELE1", "MELE CRIMSON CRISP - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 1.55, null, null, null),

                mockOrdersData.createProduct(orderType.getId(), "MELE2", "MELE OPAL - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 1.70, 2.0, null, null),

                mockOrdersData.createProduct(orderType.getId(), "ARANCE", "ARANCE - Agrinova Bio",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, true, "KG", "Cassa", 8.0, 1.10, null, "Ordinabili solo a cassa", null),

                mockOrdersData.createProduct(orderType.getId(), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                        suppliers.get("1131"), categories.get("Ortaggi"), true, false, false, "KG", "Cassa", 11.0, 1.45, null, null, null),

                mockOrdersData.createProduct(orderType.getId(), "CIPOLLE", "CIPOLLE ROSSE - Abbiate Valerio",
                        suppliers.get("1131"), categories.get("Ortaggi"), false, false, false, "KG", "Cassa", 5.0, 1.30, null, null, null)
        );

        mockUsersData.createSimpleUser("user1", "password", "user1", "user1");

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, orderType);

        User otherManager = mockUsersData.createSimpleUser("manager2", "password", "manager2", "manager2");
        mockOrdersData.addManager(otherManager, otherOrderType);
    }

    @BeforeEach
    void setUp() {
        createdProducts = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        createdProducts.forEach(mockOrdersData::deleteProduct);
    }

    @Test
    void givenAValidProductDTO_whenCreatingProduct_thenProductIsCreated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());

        String productId = createProduct(productDTO);

        assertNotNull(productId);

        /*verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(),
                null, false, null, true, false);*/
    }

    @Test
    void givenAValidProductDTO_whenUpdatingProduct_thenProductIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());

        String productId = createProduct(productDTO);

        assertNotNull(productId);

        productDTO.setBoxOnly(true);
        productDTO.setBoxUm("Cassa");
        productDTO.setBoxWeight(BigDecimal.valueOf(10));

        String productIdUpdated = updateProduct(productId, productDTO);

        assertEquals(productId, productIdUpdated);

        /*verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(),
                null, false, null, true, false);*/
    }

    @Test
    void givenAValidProductDTO_whenDeletingProduct_thenProductIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());

        String productId = createProduct(productDTO);

        assertNotNull(productId);

        deleteProduct(productId);
        createdProducts.remove(productId);

        /*verifyCreatedOrder(orderId, orderTypeComputed.getId().toUpperCase(), orderTypeComputed.getDescription(),
                null, false, null, true, false);*/
    }

    @Test
    void givenASimpleUserLogin_whenCreatingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        mockMvcGoGas.post("/api/products", productDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotManagerLogin_whenCreatingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        mockMvcGoGas.post("/api/products", productDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenUpdatingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.put("/api/products/" + productId, productDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotManagerLogin_whenUpdatingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/products/" + productId, productDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenASimpleUserLogin_whenDeletingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.delete("/api/products/" + productId)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotManagerLogin_whenDeletingProduct_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.delete("/api/products/" + productId)
                .andExpect(status().isForbidden());
    }

    private String createProduct(ProductDTO productDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.postDTO("/api/products", productDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        String productId = creationResponse.getData().toString();
        createdProducts.add(productId);
        return productId;
    }

    private String updateProduct(String productId, ProductDTO productDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.putDTO("/api/products/" + productId, productDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        return creationResponse.getData().toString();
    }

    private void deleteProduct(String productId) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.deleteDTO("/api/products/" + productId, BasicResponseDTO.class);
        assertEquals("OK", creationResponse.getData());
    }

    private ProductDTO buildValidProductDTO(String orderTypeId) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setTypeId(orderTypeId);
        productDTO.setExternalId("externalId");
        productDTO.setFrequency("frequency");
        productDTO.setDescription("description");
        productDTO.setSupplierId(suppliers.get("1041").getId());
        productDTO.setCategoryId(categories.get("Birra").getId());
        productDTO.setAvailable(true);
        productDTO.setCancelled(false);
        productDTO.setBoxOnly(false);
        productDTO.setUm("PZ");
        productDTO.setBoxUm(null);
        productDTO.setPrice(BigDecimal.valueOf(2.345));
        productDTO.setBoxWeight(null);
        productDTO.setMultiple(null);
        productDTO.setNotes(null);

        return productDTO;
    }
}
