package eu.aequos.gogas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.dto.ProductDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.dto.SupplierDTO;
import eu.aequos.gogas.integration.api.AequosApiClient;
import eu.aequos.gogas.integration.api.AequosPriceList;
import eu.aequos.gogas.mock.MockOrdersData;
import eu.aequos.gogas.persistence.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AequosPriceListSyncIntegrationTest extends BaseGoGasIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AequosApiClient aequosApiClient;

    @BeforeEach
    void setUp() throws IOException {
        AequosPriceList aequosPriceList = objectMapper.readValue(getClass().getResourceAsStream("priceList.json"), AequosPriceList.class);
        when(aequosApiClient.getPriceList(0)).thenReturn(aequosPriceList);
    }

    @AfterEach
    void tearDown() {
        mockOrdersData.deleteAllOrderTypes();
    }

    @Test
    void givenASimpleUserLogin_whenRequestingAequosPriceListSynch_thenUnauthorizedIsReturned() throws Exception {
        mockMvcGoGas.loginAsSimpleUser();

        mockMvcGoGas.put("/api/products/12345/sync")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenANotExistingOrderType_whenRequestingAequosPriceListSynch_thenNotFoundIsReturned() throws Exception {
        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.put("/api/products/" + UUID.randomUUID() + "/sync")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenANotAequosOrderType_whenRequestingAequosPriceListSynch_thenNotFoundIsReturned() throws Exception {
        OrderType orderType = mockOrdersData.createOrderType("Fresco Settimanale");

        mockMvcGoGas.loginAsAdmin();

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Impossibile sincronizzare il listino: il tipo di ordine non è collegato ad Aequos")));
    }

    @Test
    void givenANotValidOrderManagerForOrderType_whenRequestingAequosPriceListSynch_thenForbiddenIsReturned() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        OrderType otherOrderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        User simpleUser = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(simpleUser, otherOrderType);

        mockMvcGoGas.loginAs("manager", "password");

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAValidOrderManagerForOrderType_whenRequestingAequosPriceListSynch_thenSyncIsPerformed() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        User simpleUser = mockUsersData.createSimpleUser("manager2", "password", "manager", "manager");
        mockOrdersData.addManager(simpleUser, orderType);

        mockMvcGoGas.loginAs("manager2", "password");

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aequosOrderId", is(0)))
                .andExpect(jsonPath("$.lastSynchro", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
    }

    @Test
    void givenAnEmptyAequosOrderType_whenRequestingAequosPriceListSynch_thenAllProductsAreCreated() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        mockMvcGoGas.loginAsAdmin();

        assertTrue(searchAllProducts(orderType).isEmpty());
        assertTrue(searchAllCategories(orderType).isEmpty());
        assertTrue(searchAllSuppliers().isEmpty());

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aequosOrderId", is(0)))
                .andExpect(jsonPath("$.lastSynchro", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        Map<String, String> categories = searchAllCategories(orderType);
        Map<String, SupplierDTO> suppliers = searchAllSuppliers();
        Map<String, ProductDTO> createdProducts = searchAllProducts(orderType).stream()
                .collect(Collectors.toMap(ProductDTO::getExternalId, Function.identity()));

        assertEquals(3, categories.size());
        assertEquals(3, suppliers.size());
        assertEquals(5, createdProducts.size());

        verifyExpectedProduct(createdProducts.get("BIRRAMBR1041"), null, orderType.getId(), "", "BIRRA AMBRATA - BRAMA ROSSA- GRAD. ALC. 6 - 500 ML  - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, null, "");

        verifyExpectedProduct(createdProducts.get("BIRRSOLE1041"), null, orderType.getId(), "Mensile", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, null, "A bassa gradazione alcolica");

        verifyExpectedProduct(createdProducts.get("FRMECRCR1054"), null, orderType.getId(), "", "MELE CRIMSON CRISP - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.55, null, "");

        verifyExpectedProduct(createdProducts.get("FRMEOPAL1054"), null, orderType.getId(), "", "MELE OPAL - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.7, null, "Pezzatura più piccola");

        verifyExpectedProduct(createdProducts.get("ORPATGIA1131"), null, orderType.getId(), "", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                suppliers.get("1131").getId(), suppliers.get("1131").getName(), categories.get("Ortaggi"), "Ortaggi", true, false, false, "KG",
                "Cassa", 11.5, 1.45, null, "");
    }

    @Test
    void givenAllExistingAequosProductsForOrderType_whenRequestingAequosPriceListSynch_thenAllProductsAreUpdatedAndCustomFlagsKept() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        mockMvcGoGas.loginAsAdmin();

        Map<String, ProductCategory> expectedCategories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", orderType.getId())),
                entry("Frutta", mockOrdersData.createCategory("Frutta", orderType.getId())),
                entry("Ortaggi", mockOrdersData.createCategory("Ortaggi", orderType.getId()))
        );

        Map<String, Supplier> expectedSuppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)")),
                entry("1131", mockOrdersData.createSupplier("1131", "ABBIATE VALERIO"))
        );

        Map<String, String> productsByExternalId = Stream.of(
                mockOrdersData.createProduct(orderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(orderType.getId(), "BIRRSOLE1041", "d", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), false, false, false, "KG", "Cassa", 6.0, 0.2, 3.0, null, "f"),
                mockOrdersData.createProduct(orderType.getId(), "FRMECRCR1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "PZ", null, 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(orderType.getId(), "FRMEOPAL1054", "d", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f"),
                mockOrdersData.createProduct(orderType.getId(), "ORPATGIA1131", "d", expectedSuppliers.get("1131"), expectedCategories.get("Ortaggi"), false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f")
        ).collect(Collectors.toMap(Product::getExternalId, Product::getId));

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aequosOrderId", is(0)))
                .andExpect(jsonPath("$.lastSynchro", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        Map<String, String> categories = searchAllCategories(orderType);
        Map<String, SupplierDTO> suppliers = searchAllSuppliers();
        Map<String, ProductDTO> createdProducts = searchAllProducts(orderType).stream()
                .collect(Collectors.toMap(ProductDTO::getExternalId, Function.identity()));

        assertEquals(expectedCategories.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getId().toUpperCase())), categories);
        assertEquals(expectedSuppliers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getId().toUpperCase())), suppliers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getId())));
        assertEquals(5, createdProducts.size());

        verifyExpectedProduct(createdProducts.get("BIRRAMBR1041"), productsByExternalId.get("BIRRAMBR1041"), orderType.getId(), "", "BIRRA AMBRATA - BRAMA ROSSA- GRAD. ALC. 6 - 500 ML  - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, true, "PZ",
                null, 1.00, 3.65, null, "");

        verifyExpectedProduct(createdProducts.get("BIRRSOLE1041"), productsByExternalId.get("BIRRSOLE1041"), orderType.getId(), "Mensile", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, 3.0, "A bassa gradazione alcolica");

        verifyExpectedProduct(createdProducts.get("FRMECRCR1054"), productsByExternalId.get("FRMECRCR1054"), orderType.getId(), "", "MELE CRIMSON CRISP - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, true, "KG",
                "Cassa", 8.5, 1.55, null, "");

        verifyExpectedProduct(createdProducts.get("FRMEOPAL1054"), productsByExternalId.get("FRMEOPAL1054"), orderType.getId(), "", "MELE OPAL - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, true, "KG",
                "Cassa", 8.5, 1.7, null, "Pezzatura più piccola");

        verifyExpectedProduct(createdProducts.get("ORPATGIA1131"), productsByExternalId.get("ORPATGIA1131"), orderType.getId(), "", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                suppliers.get("1131").getId(), suppliers.get("1131").getName(), categories.get("Ortaggi"), "Ortaggi", true, false, true, "KG",
                "Cassa", 11.5, 1.45, null, "");
    }

    @Test
    void givenExistingAequosProductsForOrderTypeNoMoreInPriceList_whenRequestingAequosPriceListSynch_thenProductsNotInPriceListBecomeNotAvailable() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        mockMvcGoGas.loginAsAdmin();

        Map<String, ProductCategory> expectedCategories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", orderType.getId())),
                entry("Frutta", mockOrdersData.createCategory("Frutta", orderType.getId()))
        );

        Map<String, Supplier> expectedSuppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)"))
        );

        Map<String, String> productsByExternalId = Stream.of(
                mockOrdersData.createProduct(orderType.getId(), "NOT_IN_PRICELIST_1", "d1", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), true, false, true, "KG", "Cassa", 2.4, 0.2, null, "n1", "f1"),
                mockOrdersData.createProduct(orderType.getId(), "NOT_IN_PRICELIST_2", "d2", expectedSuppliers.get("1041"), expectedCategories.get("Birra"), true, false, false, "KG", "Cassa", 2.5, 0.3, 3.0, null, "f2"),
                mockOrdersData.createProduct(orderType.getId(), "NOT_IN_PRICELIST_3", "d3", expectedSuppliers.get("1054"), expectedCategories.get("Frutta"), true, false, true, "PZ", null, 1.0, 0.4, null, "n3", "f3")
        ).collect(Collectors.toMap(Product::getExternalId, Product::getId));

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aequosOrderId", is(0)))
                .andExpect(jsonPath("$.lastSynchro", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        Map<String, String> categories = searchAllCategories(orderType);
        Map<String, SupplierDTO> suppliers = searchAllSuppliers();
        Map<String, ProductDTO> allProducts = searchAllProducts(orderType).stream()
                .collect(Collectors.toMap(ProductDTO::getExternalId, Function.identity()));

        assertEquals(8, allProducts.size());

        verifyExpectedProduct(allProducts.get("BIRRAMBR1041"), productsByExternalId.get("BIRRAMBR1041"), orderType.getId(), "", "BIRRA AMBRATA - BRAMA ROSSA- GRAD. ALC. 6 - 500 ML  - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, null, "");

        verifyExpectedProduct(allProducts.get("BIRRSOLE1041"), productsByExternalId.get("BIRRSOLE1041"), orderType.getId(), "Mensile", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, null, "A bassa gradazione alcolica");

        verifyExpectedProduct(allProducts.get("FRMECRCR1054"), productsByExternalId.get("FRMECRCR1054"), orderType.getId(), "", "MELE CRIMSON CRISP - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.55, null, "");

        verifyExpectedProduct(allProducts.get("FRMEOPAL1054"), productsByExternalId.get("FRMEOPAL1054"), orderType.getId(), "", "MELE OPAL - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.7, null, "Pezzatura più piccola");

        verifyExpectedProduct(allProducts.get("ORPATGIA1131"), productsByExternalId.get("ORPATGIA1131"), orderType.getId(), "", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                suppliers.get("1131").getId(), suppliers.get("1131").getName(), categories.get("Ortaggi"), "Ortaggi", true, false, false, "KG",
                "Cassa", 11.5, 1.45, null, "");

        verifyExpectedProduct(allProducts.get("NOT_IN_PRICELIST_1"), productsByExternalId.get("NOT_IN_PRICELIST_1"), orderType.getId(), "f1", "d1",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", false, false, true, "KG",
                "Cassa", 2.4, 0.2, null, "n1");

        verifyExpectedProduct(allProducts.get("NOT_IN_PRICELIST_2"), productsByExternalId.get("NOT_IN_PRICELIST_2"), orderType.getId(), "f2", "d2",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", false, false, false, "KG",
                "Cassa", 2.5, 0.3, 3.0, null);

        verifyExpectedProduct(allProducts.get("NOT_IN_PRICELIST_3"), productsByExternalId.get("NOT_IN_PRICELIST_3"), orderType.getId(), "f3", "d3",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", false, false, true, "PZ",
                null, 1.0, 0.4, null, "n3");
    }

    @Test
    void givenAnotherProductWithSameExternalIdOfDifferentOrderType_whenRequestingAequosPriceListSynch_thenTheOtherProductIsNotUpdated() throws Exception {
        OrderType orderType = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);
        OrderType otherOrderType = mockOrdersData.createOrderType("Altro");

        mockMvcGoGas.loginAsAdmin();

        ProductCategory birra = mockOrdersData.createCategory("Birra", orderType.getId());
        ProductCategory birraOther = mockOrdersData.createCategory("Birra", otherOrderType.getId());

        Map<String, Supplier> expectedSuppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL")),
                entry("other", mockOrdersData.createSupplier("other", "BIRRIFICIO"))
        );

        Product productToSync = mockOrdersData.createProduct(orderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("1041"), birra, false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f");
        Product otherProduct = mockOrdersData.createProduct(otherOrderType.getId(), "BIRRAMBR1041", "d", expectedSuppliers.get("other"), birraOther, false, false, true, "KG", "Cassa", 2.4, 0.2, null, "n", "f");

        mockMvcGoGas.put("/api/products/" + orderType.getId() + "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aequosOrderId", is(0)))
                .andExpect(jsonPath("$.lastSynchro", is(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        Map<String, String> categories = searchAllCategories(orderType);
        Map<String, SupplierDTO> suppliers = searchAllSuppliers();
        Map<String, ProductDTO> createdProducts = searchAllProducts(orderType).stream()
                .collect(Collectors.toMap(ProductDTO::getExternalId, Function.identity()));

        assertEquals(5, createdProducts.size());

        verifyExpectedProduct(createdProducts.get("BIRRAMBR1041"), productToSync.getId(), orderType.getId(), "", "BIRRA AMBRATA - BRAMA ROSSA- GRAD. ALC. 6 - 500 ML  - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, true, "PZ",
                null, 1.00, 3.65, null, "");

        verifyExpectedProduct(createdProducts.get("BIRRSOLE1041"), null, orderType.getId(), "Mensile", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                suppliers.get("1041").getId(), suppliers.get("1041").getName(), categories.get("Birra"), "Birra", true, false, false, "PZ",
                null, 1.00, 3.65, null, "A bassa gradazione alcolica");

        verifyExpectedProduct(createdProducts.get("FRMECRCR1054"), null, orderType.getId(), "", "MELE CRIMSON CRISP - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.55, null, "");

        verifyExpectedProduct(createdProducts.get("FRMEOPAL1054"), null, orderType.getId(), "", "MELE OPAL - Roncaglia",
                suppliers.get("1054").getId(), suppliers.get("1054").getName(), categories.get("Frutta"), "Frutta", true, false, false, "KG",
                "Cassa", 8.5, 1.7, null, "Pezzatura più piccola");

        verifyExpectedProduct(createdProducts.get("ORPATGIA1131"), null, orderType.getId(), "", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                suppliers.get("1131").getId(), suppliers.get("1131").getName(), categories.get("Ortaggi"), "Ortaggi", true, false, false, "KG",
                "Cassa", 11.5, 1.45, null, "");

        Map<String, String> otherCategories = searchAllCategories(otherOrderType);
        Map<String, ProductDTO> otherProducts = searchAllProducts(otherOrderType).stream()
                .collect(Collectors.toMap(ProductDTO::getExternalId, Function.identity()));

        verifyExpectedProduct(otherProducts.get("BIRRAMBR1041"), otherProduct.getId(), otherOrderType.getId(), "f", "d",
                suppliers.get("other").getId(), suppliers.get("other").getName(), otherCategories.get("Birra"), "Birra", false, false, true, "KG",
                "Cassa", 2.4, 0.2, null, "n");
    }

    private Map<String, SupplierDTO> searchAllSuppliers() throws Exception {
        return mockMvcGoGas.getDTOList("/api/supplier/list", SupplierDTO.class).stream()
                .collect(Collectors.toMap(SupplierDTO::getExternalId, Function.identity()));
    }

    private Map<String, String> searchAllCategories(OrderType orderType) throws Exception {
        return mockMvcGoGas.getDTOList("/api/category/list/" + orderType.getId(), SelectItemDTO.class).stream()
                .collect(Collectors.toMap(SelectItemDTO::getDescription, SelectItemDTO::getId));
    }

    private List<ProductDTO> searchAllProducts(OrderType orderType) throws Exception {
        Map<String, List<String>> requestParams = Map.of("category", List.of(""), "available", List.of(""), "cancelled", List.of(""));
        return mockMvcGoGas.getDTOList("/api/products/list/" + orderType.getId(), ProductDTO.class, requestParams);
    }

    private void verifyExpectedProduct(ProductDTO productDTO, String existingId, String orderTypeId, String frequency,
                                             String description, String supplierId, String supplierName, String categoryId, String categoryName,
                                             boolean available, boolean cancelled, boolean boxOnly, String um, String boxUm,
                                             Double boxWeight, Double price, Double multiple, String notes) {

        if (existingId != null)
            assertEquals(existingId, productDTO.getId());

        assertEquals(orderTypeId, productDTO.getTypeId());
        assertEquals(description, productDTO.getDescription());
        assertEquals(supplierId.toLowerCase(), productDTO.getSupplierId());
        assertEquals(supplierName, productDTO.getSupplierName());
        assertEquals(categoryId.toLowerCase(), productDTO.getCategoryId());
        assertEquals(categoryName, productDTO.getCategoryName());
        assertEquals(available, productDTO.isAvailable());
        assertEquals(cancelled, productDTO.isCancelled());
        assertEquals(boxOnly, productDTO.isBoxOnly());
        assertEquals(um, productDTO.getUm());

        if (boxUm == null)
            assertNull(productDTO.getBoxUm());
        else
            assertEquals(boxUm, productDTO.getBoxUm());

        assertEquals(boxWeight, productDTO.getBoxWeight().doubleValue(), 0.001);
        assertEquals(price, productDTO.getPrice().doubleValue(), 0.001);

        if (multiple != null)
            assertEquals(multiple, productDTO.getMultiple().doubleValue(), 0.001);
        else
            assertNull(productDTO.getMultiple());

        assertEquals(notes, productDTO.getNotes());
        assertEquals(frequency, productDTO.getFrequency());
    }
}
