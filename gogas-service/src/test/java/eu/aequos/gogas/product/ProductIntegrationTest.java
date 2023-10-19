package eu.aequos.gogas.product;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import eu.aequos.gogas.persistence.entity.Supplier;
import eu.aequos.gogas.persistence.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
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

        //Create existing products
        mockOrdersData.createProduct(orderType.getId(), "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                suppliers.get("1041"), categories.get("Birra"), true, false, false, "PZ", null, 1.0, 3.65, null, null, "Mensile");

        mockOrdersData.createProduct(orderType.getId(), "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                suppliers.get("1041"), categories.get("Birra"), false, false, false, "PZ", null, 1.0, 3.65, null, null, "Settimanale");

        mockOrdersData.createProduct(orderType.getId(), "MELE1", "MELE CRIMSON CRISP - Roncaglia",
                suppliers.get("1054"), categories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 1.55, null, null, null);

        mockOrdersData.createProduct(orderType.getId(), "MELE2", "MELE OPAL - Roncaglia",
                suppliers.get("1054"), categories.get("Frutta"), false, false, false, "KG", "Cassa", 8.5, 1.70, 2.0, null, null);

        mockOrdersData.createProduct(orderType.getId(), "ARANCE", "ARANCE - Agrinova Bio",
                suppliers.get("1054"), categories.get("Frutta"), true, false, true, "KG", "Cassa", 8.0, 1.10, null, "Ordinabili solo a cassa", null);

        mockOrdersData.createProduct(orderType.getId(), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                suppliers.get("1131"), categories.get("Ortaggi"), true, false, false, "KG", "Cassa", 11.0, 1.45, null, null, null);

        mockOrdersData.createProduct(orderType.getId(), "CIPOLLE", "CIPOLLE ROSSE - Abbiate Valerio",
                suppliers.get("1131"), categories.get("Ortaggi"), false, true, false, "KG", "Cassa", 5.0, 1.30, null, null, null);

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

        ProductDTO createdProductDTO = getProduct(productId);
        assertEquals(productId, createdProductDTO.getId());
        assertEquals(orderType.getId(), createdProductDTO.getTypeId());
        assertEquals(orderType.getDescription(), createdProductDTO.getTypeName());
        assertEquals("externalId", createdProductDTO.getExternalId());
        assertEquals("frequency", createdProductDTO.getFrequency());
        assertEquals("description", createdProductDTO.getDescription());
        assertEquals(suppliers.get("1041").getId(), createdProductDTO.getSupplierId());
        assertEquals(suppliers.get("1041").getName(), createdProductDTO.getSupplierName());
        assertEquals(categories.get("Birra").getId(), createdProductDTO.getCategoryId());
        assertEquals(categories.get("Birra").getDescription(), createdProductDTO.getCategoryName());
        assertTrue(createdProductDTO.isAvailable());
        assertFalse(createdProductDTO.isCancelled());
        assertFalse(createdProductDTO.isBoxOnly());
        assertEquals("PZ", createdProductDTO.getUm());
        assertEquals("Cassa", createdProductDTO.getBoxUm());
        assertEquals(2.35, createdProductDTO.getPrice().doubleValue(), 0.001);
        assertEquals(6.0, createdProductDTO.getBoxWeight().doubleValue(), 0.1);
        assertNull(createdProductDTO.getMultiple());
        assertEquals("some notes", createdProductDTO.getNotes());
    }

    @Test
    void givenAnOneUnitBoxWeightAndNoBoxUnit_whenCreatingProduct_thenProductIsCreated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(BigDecimal.ONE);
        productDTO.setBoxUm(null);

        String productId = createProduct(productDTO);

        assertNotNull(productId);
    }

    @Test
    void givenBoxWeightMoreThanOneAndEmptyBoxUM_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(BigDecimal.TEN);
        productDTO.setBoxUm(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenSameUMForUnitAndBox_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(BigDecimal.TEN);
        productDTO.setBoxUm("PZ");
        productDTO.setUm("PZ");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyBoxWeight_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAZeroBoxWeight_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(BigDecimal.ZERO);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANegativeBoxWeight_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setBoxWeight(BigDecimal.valueOf(-4.0));

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyPrice_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setPrice(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAZeroPrice_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setPrice(BigDecimal.ZERO);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANegativePrice_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setPrice(BigDecimal.valueOf(-4.0));

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANullDescription_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setDescription(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyDescription_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setDescription("");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenABlankDescription_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setDescription("  ");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANullUnitUM_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setUm(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyUnitUM_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setUm("");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenABlankUnitUM_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setUm("  ");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANullSupplierId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setSupplierId(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptySupplierId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setSupplierId("");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnInvalidSupplierId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setSupplierId("invalid");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANotExistingSupplierId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setSupplierId(UUID.randomUUID().toString());

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANullOrderTypeId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setTypeId(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyOrderTypeId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setTypeId("");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnInvalidOrderTypeId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setTypeId("invalid");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANotExistingOrderType_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setTypeId(UUID.randomUUID().toString());

        mockMvcGoGas.post("/api/products", productDTO)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANullCategoryId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setCategoryId(null);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnEmptyCategoryId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setCategoryId("");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAnInvalidCategoryId_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setCategoryId("invalid");

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANotExistingCategory_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setCategoryId(UUID.randomUUID().toString());

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAZeroMultiple_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setMultiple(BigDecimal.ZERO);

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenANegativeMultiple_whenCreatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        productDTO.setMultiple(BigDecimal.valueOf(-4.0));

        createProductWithValidationError(productDTO);
    }

    @Test
    void givenAValidProductDTO_whenUpdatingProduct_thenProductIsUpdated() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        assertNotNull(productId);

        productDTO.setBoxOnly(true);
        productDTO.setBoxUm("Cartoni");
        productDTO.setBoxWeight(BigDecimal.valueOf(10));

        String productIdUpdated = updateProduct(productId, productDTO);

        assertEquals(productId, productIdUpdated);

        ProductDTO updatedProductDTO = getProduct(productId);
        assertEquals("Cartoni", updatedProductDTO.getBoxUm());
        assertEquals(10.0, updatedProductDTO.getBoxWeight().doubleValue(), 0.1);
        assertTrue(updatedProductDTO.isBoxOnly());
    }

    @Test
    void givenBoxWeightMoreThanOneAndEmptyBoxUM_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setBoxWeight(BigDecimal.TEN);
        productDTO.setBoxUm(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenSameUMForUnitAndBox_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setBoxWeight(BigDecimal.TEN);
        productDTO.setBoxUm("PZ");
        productDTO.setUm("PZ");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyBoxWeight_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setBoxWeight(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAZeroBoxWeight_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setBoxWeight(BigDecimal.ZERO);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANegativeBoxWeight_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setBoxWeight(BigDecimal.valueOf(-4.0));

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyPrice_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setPrice(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAZeroPrice_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setPrice(BigDecimal.ZERO);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANegativePrice_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setPrice(BigDecimal.valueOf(-4.0));

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANullDescription_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setDescription(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyDescription_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setDescription("");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenABlankDescription_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setDescription("  ");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANullUnitUM_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setUm(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyUnitUM_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setUm("");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenABlankUnitUM_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setUm("  ");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANullSupplierId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setSupplierId(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptySupplierId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setSupplierId("");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnInvalidSupplierId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setSupplierId("invalid");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANotExistingSupplierId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setSupplierId(UUID.randomUUID().toString());

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANullOrderTypeId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setTypeId(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyOrderTypeId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setTypeId("");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnInvalidOrderTypeId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setTypeId("invalid");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANotExistingOrderType_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setTypeId(UUID.randomUUID().toString());

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANullCategoryId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setCategoryId(null);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnEmptyCategoryId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setCategoryId("");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAnInvalidCategoryId_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setCategoryId("invalid");

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANotExistingCategory_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setCategoryId(UUID.randomUUID().toString());

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAZeroMultiple_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setMultiple(BigDecimal.ZERO);

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenANegativeMultiple_whenUpdatingProduct_thenValidationErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());
        String productId = createProduct(productDTO);

        productDTO.setMultiple(BigDecimal.valueOf(-4.0));

        updateProductWithValidationError(productId, productDTO);
    }

    @Test
    void givenAValidProductDTO_whenDeletingProduct_thenProductIsDeleted() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        ProductDTO productDTO = buildValidProductDTO(orderType.getId());

        String productId = createProduct(productDTO);

        assertNotNull(productId);

        deleteProduct(productId);

        mockMvcGoGas.get("/api/products/" + productId)
                .andExpect(status().isForbidden());

        createdProducts.remove(productId);
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

    @Test
    void givenAListOfProducts_whenSearchingProducts_thenProductsAreReturnedInAlphabeticalOrder() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of(
                "ARANCE - Agrinova Bio",
                "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                "CIPOLLE ROSSE - Abbiate Valerio",
                "MELE CRIMSON CRISP - Roncaglia",
                "MELE OPAL - Roncaglia",
                "PATATE GIALLE DI MONTAGNA - Abbiate Valerio"
        );

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenAFilterOnCategory_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("category", List.of(categories.get("Birra").getId()));
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of(
                "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone"
        );

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenAFilterOnInvalidCategory_whenSearchingProducts_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("category", List.of("invalid"));

        mockMvcGoGas.get("/api/products/list/" + orderType.getId(), new LinkedMultiValueMap<>(params))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAFilterOnNotExistingCategory_whenSearchingProducts_thenNoProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of(
                "category", List.of(UUID.randomUUID().toString())
        );

        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        assertTrue(actualProductNames.isEmpty());
    }

    @Test
    void givenAFilterOnAvailableOnly_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("available", List.of("true"));
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of(
                "ARANCE - Agrinova Bio",
                "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                "MELE CRIMSON CRISP - Roncaglia",
                "PATATE GIALLE DI MONTAGNA - Abbiate Valerio"
        );

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenAFilterOnNotAvailableOnly_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("available", List.of("false"));
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of(
                "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                "CIPOLLE ROSSE - Abbiate Valerio",
                "MELE OPAL - Roncaglia"
        );

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenAFilterOnInvalidFlagValue_whenSearchingProducts_thenErrorIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("available", List.of("invalid"));

        mockMvcGoGas.get("/api/products/list/" + orderType.getId(), new LinkedMultiValueMap<>(params))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAFilterOnNotCancelledOnly_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("cancelled", List.of("false"));
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of(
                "ARANCE - Agrinova Bio",
                "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                "MELE CRIMSON CRISP - Roncaglia",
                "MELE OPAL - Roncaglia",
                "PATATE GIALLE DI MONTAGNA - Abbiate Valerio"
        );

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenAFilterOnCancelledOnly_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of("cancelled", List.of("true"));
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of("CIPOLLE ROSSE - Abbiate Valerio");

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenACompositeFilterThatMatchesProducts_whenSearchingProducts_thenProductsAreCorrectlyFiltered() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of(
                "category", List.of(categories.get("Ortaggi").getId()),
                "available", List.of("false"),
                "cancelled", List.of("true")
        );
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        List<String> expectedProductNames = List.of("CIPOLLE ROSSE - Abbiate Valerio");

        assertEquals(expectedProductNames, actualProductNames);
    }

    @Test
    void givenACompositeFilterThatMatchesNoProducts_whenSearchingProducts_thenNoProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        Map<String, List<String>> params = Map.of(
                "category", List.of(categories.get("Birra").getId()),
                "available", List.of("false"),
                "cancelled", List.of("true")
        );
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, params);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        assertTrue(actualProductNames.isEmpty());
    }

    @Test
    void givenASimpleUserLogin_whenSearchingProducts_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("user1", "password");

        mockMvcGoGas.get("/api/products/list/" + orderType.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotManagerLogin_whenSearchingProducts_thenNotAuthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.get("/api/products/list/" + orderType.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnInvalidOrderTypeId_whenSearchingProducts_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/products/list/invalid")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderTypeId_whenSearchingProducts_thenForbiddenIsReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.get("/api/products/list/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnUpperCaseOrderTypeId_whenSearchingProducts_thenProductsAreReturned() throws Exception {
        mockMvcGoGas.loginAs("manager", "password");
        List<ProductDTO> products = mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId().toUpperCase(), ProductDTO.class);

        List<String> actualProductNames = products.stream().map(ProductDTO::getDescription).collect(Collectors.toList());
        assertEquals(7, actualProductNames.size());
    }

    private String createProduct(ProductDTO productDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.postDTO("/api/products", productDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        String productId = creationResponse.getData().toString();
        createdProducts.add(productId);
        return productId;
    }

    public void createProductWithValidationError(ProductDTO productDTO) throws Exception {
        mockMvcGoGas.post("/api/products", productDTO)
                .andExpect(status().isBadRequest());
    }

    private ProductDTO getProduct(String productId) throws Exception {
        return mockMvcGoGas.getDTO("/api/products/" + productId, ProductDTO.class);
    }

    private String updateProduct(String productId, ProductDTO productDTO) throws Exception {
        BasicResponseDTO creationResponse = mockMvcGoGas.putDTO("/api/products/" + productId, productDTO, BasicResponseDTO.class);
        assertNotNull(creationResponse.getData());
        return creationResponse.getData().toString();
    }

    public void updateProductWithValidationError(String productId, ProductDTO productDTO) throws Exception {
        mockMvcGoGas.put("/api/products/" + productId, productDTO)
                .andExpect(status().isBadRequest());
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
        productDTO.setBoxUm("Cassa");
        productDTO.setPrice(BigDecimal.valueOf(2.345));
        productDTO.setBoxWeight(BigDecimal.valueOf(6.0));
        productDTO.setMultiple(null);
        productDTO.setNotes("some notes");

        return productDTO;
    }

    //TODO: add tests for import/export from excel
}
