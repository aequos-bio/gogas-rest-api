package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared extraction of the per-user "order sheet" data used by both the PDF
 * ({@link PdfReportService}) and the Excel ({@link ExcelUserSheetsService})
 * exports of {@code /api/order/manage/{orderId}/byUser/sheets}.
 *
 * Centralizes the data loading, user/product selection and the box/quantity
 * formatting rules so the two outputs stay identical.
 */
@Service
@RequiredArgsConstructor
public class UserOrderSheetsExtractor {

    private static final DecimalFormat QUANTITY_FORMAT =
            new DecimalFormat("0.000", new DecimalFormatSymbols(Locale.ITALIAN));

    private final OrderItemRepo orderItemRepo;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;

    /**
     * Build the ordered list of per-user sheets for an order. Users are listed
     * in the same order as {@link UserRepo#findByIdIn}; products within each
     * sheet follow the price-list order and are grouped by category.
     */
    public List<UserOrderSheetData> extract(Order order) {
        List<OrderItem> orderItems = orderItemRepo.findByOrderAndSummary(order.getId(), true);
        List<User> usersList = getUsersForExport(orderItems);
        List<Product> productsForExport = getProductsForExport(orderItems);

        List<UserOrderSheetData> sheets = new ArrayList<>();
        for (User user : usersList) {
            List<UserOrderSheetData.Item> items = extractUserItems(orderItems, user, productsForExport);
            sheets.add(new UserOrderSheetData(user.getFirstName(), user.getLastName(), user.getPosition(), items));
        }
        return sheets;
    }

    private List<UserOrderSheetData.Item> extractUserItems(List<OrderItem> orderItems, User user,
                                                           List<Product> productsForExport) {
        Map<String, OrderItem> userOrderItemsByProduct = orderItems.stream()
                .filter(i -> i.getUser().equals(user.getId()))
                .collect(Collectors.toMap(OrderItem::getProduct, Function.identity()));

        return productsForExport.stream()
                .filter(product -> userOrderItemsByProduct.containsKey(product.getId()))
                .map(product -> toItem(userOrderItemsByProduct.get(product.getId()), product))
                .collect(Collectors.toList());
    }

    private UserOrderSheetData.Item toItem(OrderItem item, Product product) {
        return new UserOrderSheetData.Item(
                product.getCategory().getDescription(),
                product.getDescription(),
                product.getUm(),
                item.getPrice(),
                formatQuantity(item, product));
    }

    private static String formatQuantity(OrderItem item, Product product) {
        if (isCompleteBox(item, product)) {
            int boxes = (int) (item.getOrderedQuantity().doubleValue() / product.getBoxWeight().doubleValue());
            return boxes + " cass" + (boxes > 1.0 ? 'e' : 'a');
        }

        return QUANTITY_FORMAT.format(item.getDeliveredQuantity());
    }

    private static boolean isCompleteBox(OrderItem item, Product product) {
        if (product.getBoxUm() == null) {
            return false;
        }

        if (product.getBoxWeight().doubleValue() == 1.0) {
            return false;
        }

        double remainder = item.getOrderedQuantity().doubleValue() % product.getBoxWeight().doubleValue();
        return remainder == 0.0;
    }

    private List<User> getUsersForExport(List<OrderItem> orderItems) {
        Set<String> userIdsInOrder = orderItems.stream()
                .map(OrderItem::getUser)
                .collect(Collectors.toSet());

        return userRepo.findByIdIn(userIdsInOrder, User.class);
    }

    private List<Product> getProductsForExport(List<OrderItem> orderItems) {
        Set<String> productIdsInOrder = orderItems.stream()
                .map(OrderItem::getProduct)
                .collect(Collectors.toSet());

        return productRepo.findByIdInOrderByPriceList(productIdsInOrder);
    }
}
