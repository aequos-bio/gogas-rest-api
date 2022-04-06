package eu.aequos.gogas.order;

import eu.aequos.gogas.BaseGoGasIntegrationTest;
import eu.aequos.gogas.persistence.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class OrderBaseIntegrationTest extends BaseGoGasIntegrationTest {

    protected OrderType orderTypeComputed;
    protected OrderType orderTypeNotComputed;
    protected OrderType orderTypeExternal;
    protected OrderType orderTypeAequos;

    protected Map<String, Product> productsByCodeComputed;
    protected Map<String, Product> productsByCodeNotComputed;

    protected String userId1;
    protected String userId2;
    protected String userId3;
    protected String friendId1a;
    protected String friendId1b;
    protected String friendId2;
    protected String orderManagerId1;
    protected String orderManagerId2;

    protected List<String> createdOrderIds;

    @BeforeAll
    void createOrderTypeAndUsers() {
        orderTypeComputed = mockOrdersData.createOrderType("Fresco Settimanale", true);

        Map<String, ProductCategory> categories = Map.ofEntries(
                entry("Birra", mockOrdersData.createCategory("Birra", orderTypeComputed.getId(), 3, "white")),
                entry("Frutta", mockOrdersData.createCategory("Frutta", orderTypeComputed.getId(), 1, "green")),
                entry("Ortaggi", mockOrdersData.createCategory("Ortaggi", orderTypeComputed.getId(), 2, "red"))
        );

        Map<String, Supplier> suppliers = Map.ofEntries(
                entry("1041", mockOrdersData.createSupplier("1041", "BIRRIFICIO ARTIGIANALE GEDEONE SRL", "TN")),
                entry("1054", mockOrdersData.createSupplier("1054", "Az. Agr. BIANCIOTTO ALDO (Roncaglia Bio)", "MN")),
                entry("1131", mockOrdersData.createSupplier("1131", "ABBIATE VALERIO", "BO"))
        );

        List<Product> products = List.of(
                mockOrdersData.createProduct(orderTypeComputed.getId(), "BIRRA1", "BIRRA AMBRATA - BRAMA ROSSA - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), true, false, false, "PZ", null, 1.0, 3.65, null, null, "Mensile"),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "BIRRA2", "BIRRA SOLEA 3,8gradi - 500 ML - Birrificio Gedeone",
                        suppliers.get("1041"), categories.get("Birra"), true, false, false, "PZ", null, 1.0, 3.65, null, null, "Settimanale"),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "MELE1", "MELE CRIMSON CRISP - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 1.55, null, null, null),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "MELE2", "MELE OPAL - Roncaglia",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, false, "KG", "Cassa", 8.5, 1.70, 2.0, null, null),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "ARANCE", "ARANCE - Agrinova Bio",
                        suppliers.get("1054"), categories.get("Frutta"), true, false, true, "KG", "Cassa", 8.0, 1.10, null, "Ordinabili solo a cassa", null),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "PATATE", "PATATE GIALLE DI MONTAGNA - Abbiate Valerio",
                        suppliers.get("1131"), categories.get("Ortaggi"), true, false, false, "KG", "Cassa", 11.0, 1.45, null, null, null),

                mockOrdersData.createProduct(orderTypeComputed.getId(), "CIPOLLE", "CIPOLLE ROSSE - Abbiate Valerio",
                        suppliers.get("1131"), categories.get("Ortaggi"), false, false, false, "KG", "Cassa", 5.0, 1.30, null, null, null)
        );

        productsByCodeComputed = products.stream().collect(Collectors.toMap(Product::getExternalId, Function.identity()));

        orderTypeNotComputed = mockOrdersData.createOrderType("Cirenaica", false);

        Map<String, ProductCategory> categoriesNotComputed = Map.ofEntries(
                entry("Carne Fresca", mockOrdersData.createCategory("Carne Fresca", orderTypeNotComputed.getId())),
                entry("Bovino", mockOrdersData.createCategory("Bovino", orderTypeNotComputed.getId())),
                entry("Affettati", mockOrdersData.createCategory("Affettati", orderTypeNotComputed.getId()))
        );

        Supplier supplierNotComputed = mockOrdersData.createSupplier("1111", "Cirenaica");


        List<Product> productsNotComputed = List.of(
                mockOrdersData.createProduct(orderTypeNotComputed.getId(), "COSTINE", "Costine",
                        supplierNotComputed, categoriesNotComputed.get("Carne Fresca"), "KG", null, 1.0, 5.0),

                mockOrdersData.createProduct(orderTypeNotComputed.getId(), "FILETTO", "Filetto di maiale",
                        supplierNotComputed, categoriesNotComputed.get("Carne Fresca"), "KG", null, 1.0, 4.5),

                mockOrdersData.createProduct(orderTypeNotComputed.getId(), "FEGATO", "MFegato di Bovino",
                        supplierNotComputed, categoriesNotComputed.get("Bovino"), "KG", null, 1.0, 10.6),

                mockOrdersData.createProduct(orderTypeNotComputed.getId(), "FETTINE", "Fettine",
                        supplierNotComputed, categoriesNotComputed.get("Bovino"), "KG", null, 1.0, 4.85),

                mockOrdersData.createProduct(orderTypeNotComputed.getId(), "COPPA", "Coppa stagionata",
                        supplierNotComputed, categoriesNotComputed.get("Affettati"), "KG", null, 1.0, 8.3)
        );

        productsByCodeNotComputed = productsNotComputed.stream().collect(Collectors.toMap(Product::getExternalId, Function.identity()));

        orderTypeExternal = mockOrdersData.createExternalOrderType("Tomasoni");

        orderTypeAequos = mockOrdersData.createAequosOrderType("Fresco Settimanale", 0);

        User user1 = mockUsersData.createSimpleUser("user1", "password", "user1", "user1");
        User user2 = mockUsersData.createSimpleUser("user2", "password", "user2", "user2");
        User user3 = mockUsersData.createSimpleUser("user3", "password", "user3", "user3");

        userId1 = user1.getId().toUpperCase();
        userId2 = user2.getId().toUpperCase();
        userId3 = user3.getId().toUpperCase();

        friendId1a = mockUsersData.createFriendUser("friendId1a", "password", "friendId1a", "friendId1a", user1).getId().toUpperCase();
        friendId1b = mockUsersData.createFriendUser("friendId1b", "password", "friendId1b", "friendId1b", user1).getId().toUpperCase();
        friendId2 = mockUsersData.createFriendUser("friendId2", "password", "friendId2", "friendId2", user2).getId().toUpperCase();

        User orderManager = mockUsersData.createSimpleUser("manager", "password", "manager", "manager");
        mockOrdersData.addManager(orderManager, orderTypeComputed);
        mockOrdersData.addManager(orderManager, orderTypeExternal);
        orderManagerId1 = orderManager.getId();

        User otherManager = mockUsersData.createSimpleUser("manager2", "password", "manager2", "manager2");
        mockOrdersData.addManager(otherManager, orderTypeNotComputed);
        mockOrdersData.addManager(otherManager, orderTypeAequos);
        orderManagerId2 = otherManager.getId();
    }

    @BeforeEach
    void setUp() {
        createdOrderIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        createdOrderIds.forEach(mockOrdersData::deleteOrder);
    }
}
