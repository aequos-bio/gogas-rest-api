package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsManager;
import eu.aequos.gogas.security.annotations.IsOrderManager;
import eu.aequos.gogas.security.annotations.IsOrderTypeManager;
import eu.aequos.gogas.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/order/manage")
@IsOrderManager
public class OrderManagerController {

    private ExcelGenerationService reportService;
    private OrderManagerService orderManagerService;
    private OrderItemService orderItemService;
    private AuthorizationService authorizationService;
    private BuyersReportService buyersReportService;
    private ConfigurationService configurationService;

    public OrderManagerController(ExcelGenerationService reportService, OrderManagerService orderManagerService,
                                  OrderItemService orderItemService, AuthorizationService authorizationService,
                                  BuyersReportService buyersReportService, ConfigurationService configurationService) {

        this.reportService = reportService;
        this.orderManagerService = orderManagerService;
        this.orderItemService = orderItemService;
        this.authorizationService = authorizationService;
        this.buyersReportService = buyersReportService;
        this.configurationService = configurationService;
    }

    @IsManager
    @PostMapping(value = "list")
    public List<OrderDTO> listOrders(@RequestBody OrderSearchFilter searchFilter) {
        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderManagerService.search(searchFilter, userId, userRole);
    }

    @GetMapping(value = "{orderId}")
    public OrderDetailsDTO getOrderDetails(@PathVariable String orderId) throws ItemNotFoundException {
        return orderManagerService.getOrderDetails(orderId);
    }

    @GetMapping(value = "{orderId}/product")
    public List<OrderByProductDTO> getOrderDetailsByProduct(@PathVariable String orderId) throws ItemNotFoundException {
        return orderManagerService.getOrderDetailByProduct(orderId);
    }

    @GetMapping(value = "{orderId}/product/{productId}")
    public List<OrderItemByProductDTO> getProductDetails(@PathVariable String orderId, @PathVariable String productId) throws ItemNotFoundException {
        return orderManagerService.getOrderItemsByProduct(orderId, productId);
    }


    //TODO: check come fare per token
    @GetMapping(value = "{orderId}/export")
    public void getUserOrderItems(HttpServletResponse response, @PathVariable String orderId) throws IOException, ItemNotFoundException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.getOutputStream().write(reportService.extractOrderDetails(orderId));
        response.getOutputStream().flush();
    }

    @PreAuthorize("@authorizationService.isOrderTypeManager(#orderDTO.orderTypeId)")
    @PostMapping()
    public String create(@RequestBody OrderDTO orderDTO) throws GoGasException {
        return orderManagerService.create(orderDTO).getId();
    }

    @PutMapping(value = "{orderId}")
    public String update(@PathVariable String orderId, @RequestBody OrderDTO orderDTO) throws ItemNotFoundException {
        return orderManagerService.update(orderId, orderDTO).getId();
    }

    @DeleteMapping(value = "{orderId}")
    public void delete(@PathVariable String orderId) {
        orderManagerService.delete(orderId);
    }

    @PostMapping(value = "{orderId}/action/{actionCode}")
    public String update(@PathVariable String orderId, @PathVariable String actionCode,
                         @RequestParam(required = false, defaultValue = "0") int roundType) throws ItemNotFoundException, InvalidOrderActionException {

        orderManagerService.changeStatus(orderId, actionCode, roundType);
        return "OK";
    }

    @IsOrderManager
    @PutMapping(value = "{orderId}/item/{itemId}")
    public BasicResponseDTO updateQty(@PathVariable String orderId, @PathVariable String itemId, @RequestBody BigDecimal qty) throws ItemNotFoundException {
        if (!orderManagerService.updateItemDeliveredQty(orderId, itemId, qty))
             throw new ItemNotFoundException("orderItem", itemId);

        return new BasicResponseDTO("OK");
    }

    @PostMapping(value = "{orderId}/item")
    public BasicResponseDTO insertOrderItem(@PathVariable String orderId, @RequestBody OrderItemUpdateRequest orderItem) throws GoGasException {
        orderManagerService.insertOrderItem(orderId, orderItem);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "{orderId}/product/{productId}/availableUsers")
    public List<SelectItemDTO> getUsersNotOrdering(@PathVariable String orderId, @PathVariable String productId) throws ItemNotFoundException {
        return orderManagerService.getUsersNotOrdering(orderId, productId);
    }

    @GetMapping(value = "{orderId}/byUser/list")
    public List<OrderByUserDTO> getOrderDetailsByUser(@PathVariable String orderId) throws ItemNotFoundException {
        return orderManagerService.getOrderDetailByUser(orderId);
    }

    @GetMapping(value = "{orderId}/byUser/availableUsers")
    public List<SelectItemDTO> getUsersNotOrdering(@PathVariable String orderId) throws ItemNotFoundException {
        return orderManagerService.getUsersNotOrdering(orderId);
    }

    @GetMapping(value = "{orderId}/byUser/{userId}")
    public List<OrderItemByUserDTO> getByUserDetails(@PathVariable String orderId, @PathVariable String userId) throws ItemNotFoundException {
        return orderManagerService.getOrderItemsByUser(orderId, userId);
    }

    @PostMapping(value = "{orderId}/byUser/{userId}")
    public String updateAmountByUser(@PathVariable String orderId, @PathVariable String userId, @RequestBody BigDecimal cost) throws ItemNotFoundException {
        return orderManagerService.updateUserCost(orderId, userId, cost);
    }

    @DeleteMapping(value = "{orderId}/byUser/{userId}")
    public BasicResponseDTO deleteEntryByUser(@PathVariable String orderId, @PathVariable String userId) {
        orderManagerService.deleteUserCost(orderId, userId);
        return new BasicResponseDTO("OK");

    }

    @PostMapping(value = "{orderId}/shippingCost")
    public List<OrderByUserDTO> updateShippingCost(@PathVariable String orderId, @RequestBody BigDecimal cost) throws ItemNotFoundException {
        return orderManagerService.updateShippingCost(orderId, cost);
    }

    @PostMapping(value = "{orderId}/invoice")
    public BasicResponseDTO updateInvoiceData(@PathVariable String orderId, @RequestBody OrderInvoiceDataDTO invoiceData) throws GoGasException {
        orderManagerService.updateInvoiceData(orderId, invoiceData);
        return new BasicResponseDTO("OK");
    }

    @IsManager
    @GetMapping(value = "aequos/available")
    public List<OrderDTO> getAequosAvailableOpenOrders() {
        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderManagerService.getAequosAvailableOpenOrders(userId, userRole);
    }

    /********************************/

    @PutMapping(value = "{orderId}/product/{productId}/supplier")
    public BasicResponseDTO updateSupplierOrderQty(@PathVariable String orderId, @PathVariable String productId, @RequestBody int boxes) {
        orderManagerService.updateSupplierOrderQty(orderId, productId, boxes);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/cancel")
    public BasicResponseDTO cancelProductOrder(@PathVariable String orderId, @PathVariable String productId) {
        orderItemService.cancelProductOrder(orderId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/restore")
    public BasicResponseDTO restoreProductOrder(@PathVariable String orderId, @PathVariable String productId) {
        orderItemService.restoreProductOrder(orderId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/item/{orderItemId}/cancel")
    public BasicResponseDTO cancelOrderItem(@PathVariable String orderId, @PathVariable String orderItemId) {
        orderItemService.cancelOrderItem(orderItemId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/item/{orderItemId}/restore")
    public BasicResponseDTO restoreOrderItem(@PathVariable String orderId, @PathVariable String orderItemId) {
        orderItemService.restoreOrderItem(orderItemId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/item/{orderItemId}/replace")
    public BasicResponseDTO replaceProductOrder(@PathVariable String orderId, @PathVariable String orderItemId, @RequestBody String productId) throws ItemNotFoundException {
        orderManagerService.replaceOrderItemWithProduct(orderId, orderItemId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/distribute")
    public BasicResponseDTO distributeRemainingQuantities(@PathVariable String orderId, @PathVariable String productId) throws ItemNotFoundException {
        orderManagerService.distributeRemainingQuantities(orderId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/price")
    public BasicResponseDTO updateProductPrice(@PathVariable String orderId, @PathVariable String productId, @RequestBody BigDecimal price) throws GoGasException {
        orderManagerService.updateProductPrice(orderId, productId, price);
        return new BasicResponseDTO("OK");
    }

    /**************** REPORT ******************/

    @IsOrderTypeManager
    @GetMapping(value = "{productTypeId}/report/buyers")
    public BuyersReportDTO generateBuyersReport(@PathVariable String productTypeId, @RequestParam(required = false) String dateFrom, @RequestParam(required = false) String dateTo) {
        LocalDate parsedDateFrom = configurationService.parseLocalDate(dateFrom);
        LocalDate parsedDateTo = configurationService.parseLocalDate(dateTo);
        return buyersReportService.generateBuyersReport(productTypeId, parsedDateFrom, parsedDateTo);
    }
}
