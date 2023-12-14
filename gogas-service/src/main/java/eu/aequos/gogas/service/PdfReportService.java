package eu.aequos.gogas.service;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.entity.OrderItem;
import eu.aequos.gogas.persistence.entity.Product;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderItemRepo;
import eu.aequos.gogas.persistence.repository.ProductRepo;
import eu.aequos.gogas.persistence.repository.UserRepo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.itextpdf.layout.properties.TextAlignment.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class PdfReportService {

    private static final float[] COLUMN_WIDTHS = new float[] { 400F, 30F, 50F, 50F, 50F };
    private static final SolidBorder TABLE_BORDER = new SolidBorder(new DeviceGray(0.75f), 0.75f);
    private static final DeviceGray CATEGORY_COLOR = new DeviceGray(0.96f);
    private static final DeviceRgb ODD_COLOR = new DeviceRgb(244, 245, 220);

    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("0.000", new DecimalFormatSymbols(Locale.ITALIAN));
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("€ 0.00", new DecimalFormatSymbols(Locale.ITALIAN));

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderItemRepo orderItemRepo;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;

    public byte[] generateUserSheets(Order order) {
        List<OrderItem> orderItems = orderItemRepo.findByOrderAndSummary(order.getId(), true);
        List<User> usersList = getUsersForExport(orderItems);
        List<Product> productsForExport = getProductsForExport(orderItems);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            for (int userIndex = 0; userIndex < usersList.size(); userIndex++) {
                User user = usersList.get(userIndex);
                List<OrderItemPdfExport> orderItemsForExport = extractUserOrderItems(orderItems, user, productsForExport);

                doc.add(buildUserTable(doc, user, order, orderItemsForExport));

                if (userIndex < usersList.size() - 1) {
                    doc.add(new AreaBreak());
                }
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Error while generating user PDF sheets for order " + order.getId(), ex);
            throw new GoGasException();
        }
    }

    private List<OrderItemPdfExport> extractUserOrderItems(List<OrderItem> orderItems, User user, List<Product> productsForExport) {
        Map<String, OrderItem> userOrderItemsByProduct = orderItems.stream()
                .filter(i -> i.getUser().equals(user.getId()))
                .collect(Collectors.toMap(OrderItem::getProduct, Function.identity()));

        return productsForExport.stream()
                .filter(product -> userOrderItemsByProduct.containsKey(product.getId()))
                .map(product -> getOrderItemsForExport(userOrderItemsByProduct.get(product.getId()), product))
                .collect(Collectors.toList());
    }

    private Table buildUserTable(Document doc, User user, Order order, List<OrderItemPdfExport> orderItemsForExport) {
        doc.add(buildTitle(user));

        Table table = new Table(COLUMN_WIDTHS);
        table.setBorder(TABLE_BORDER);

        table.addHeaderCell(buildUserHeaderCell(user, order));

        table.addCell(buildHeaderCell("Prodotto", LEFT));
        table.addCell(buildHeaderCell("UM", CENTER));
        table.addCell(buildHeaderCell("Prezzo al KG", CENTER));
        table.addCell(buildHeaderCell("Qta ordinata", CENTER));
        table.addCell(buildHeaderCell("Qta consegnata", CENTER));

        String currentCategory = "";

        for (int index = 0; index < orderItemsForExport.size(); index++) {
            OrderItemPdfExport orderItemExport = orderItemsForExport.get(index);

            if (!currentCategory.equals(orderItemExport.getCategory())) {
                table.addCell(buildCategoryCell(orderItemExport.getCategory()));
                currentCategory = orderItemExport.getCategory();
            }

            boolean odd = index % 2 == 1;

            table.addCell(buildTextCell(orderItemExport.getProductName(), odd));
            table.addCell(buildNumericCell(orderItemExport.getUm(), odd));
            table.addCell(buildNumericCell(PRICE_FORMAT.format(orderItemExport.getUnitPrice()), odd));
            table.addCell(buildNumericCell(orderItemExport.getFormattedQuantity(), odd));
            table.addCell(buildNumericCell("", odd));
        }

        table.addCell(new Cell(1, 5)
                .setBorderLeft(new SolidBorder(new DeviceRgb(255, 255, 255), 0.75F))
                .setBorderRight(new SolidBorder(new DeviceRgb(255, 255, 255), 0.75F))
                .setHeight(16.0F));

        table.addCell(buildFooterCell("Totale da pagare:"));
        table.addCell(emptyCell());

        table.addCell(buildFooterCell("Numero di cassette da ritirare:"));
        table.addCell(emptyCell());

        table.addCell(buildFooterCell("Cassette confezionate per te da:"));
        table.addCell(emptyCell());

        return table;
    }

    private Cell buildUserHeaderCell(User user, Order order) {
        String text = "Ordine " + order.getOrderType().getDescription() + " in consegna " + DATE_FORMAT.format(order.getDeliveryDate()) +
                " - " + user.getFirstName() + " " + user.getLastName();

        return new Cell(1, 5)
                .add(new Paragraph(text))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(TABLE_BORDER);
    }

    private Table buildTitle(User user) {
        Cell titleCell = new Cell()
                .add(new Paragraph(Integer.toString(user.getPosition())))
                .setFontSize(14)
                .setTextAlignment(CENTER)
                .setBorder(Border.NO_BORDER);

        return new Table(new float[] { 1000F })
                .setBorder(Border.NO_BORDER)
                .addCell(titleCell);
    }

    private Cell emptyCell() {
        return buildCell(2, "", false, CENTER);
    }

    private Cell buildFooterCell(String text) {
        return buildCell(3, text, false, RIGHT)
                .setBold()
                .setItalic();
    }

    private Cell buildCategoryCell(String text) {
        return buildCell(5, text, false, LEFT)
                .setItalic()
                .setBackgroundColor(CATEGORY_COLOR);
    }

    private Cell buildHeaderCell(String text, TextAlignment textAlignment) {
        return buildCell(1, text, false, textAlignment)
                .setBold();
    }

    private Cell buildTextCell(String text, boolean odd) {
        return buildCell(1, text, odd, LEFT);
    }

    private Cell buildNumericCell(String text, boolean odd) {
        return buildCell(1, text, odd, TextAlignment.CENTER);
    }

    private Cell buildCell(int colSpan, String text, boolean odd, TextAlignment alignment) {
        Cell cell = new Cell(1, colSpan)
                .add(new Paragraph(text))
                .setFontSize(9)
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(TABLE_BORDER);

        if (odd) {
            cell.setBackgroundColor(ODD_COLOR);
        }

        return cell;
    }

    private OrderItemPdfExport getOrderItemsForExport(OrderItem item, Product product) {
        OrderItemPdfExport exportItem = new OrderItemPdfExport();
        exportItem.setProductName(product.getDescription());
        exportItem.setUm(product.getUm());
        exportItem.setCategory(product.getCategory().getDescription());
        exportItem.setFormattedQuantity(formatQuantity(item, product));
        exportItem.setUnitPrice(item.getPrice());
        return exportItem;
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

    @Data
    private static class OrderItemPdfExport {
        private String productName;
        private String um;
        private String category;
        private BigDecimal unitPrice;
        private String formattedQuantity;
    }
}
