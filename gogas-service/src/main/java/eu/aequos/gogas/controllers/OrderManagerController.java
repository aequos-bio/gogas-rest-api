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
import eu.aequos.gogas.service.BuyersReportService;
import eu.aequos.gogas.service.ConfigurationService;
import eu.aequos.gogas.service.OrderItemService;
import eu.aequos.gogas.service.OrderManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.PositiveOrZero;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Api("Order manager operations")
@RestController
@Validated
@IsOrderManager
@RequestMapping("api/order/manage")
public class OrderManagerController {

    private OrderManagerService orderManagerService;
    private OrderItemService orderItemService;
    private AuthorizationService authorizationService;
    private BuyersReportService buyersReportService;
    private ConfigurationService configurationService;

    public OrderManagerController(OrderManagerService orderManagerService,
                                  OrderItemService orderItemService, AuthorizationService authorizationService,
                                  BuyersReportService buyersReportService, ConfigurationService configurationService) {

        this.orderManagerService = orderManagerService;
        this.orderItemService = orderItemService;
        this.authorizationService = authorizationService;
        this.buyersReportService = buyersReportService;
        this.configurationService = configurationService;
    }

    @ApiOperation(
        value = "Search order",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @IsManager
    @PostMapping(value = "list")
    public List<OrderDTO> listOrders(@RequestBody OrderSearchFilter searchFilter) {
        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderManagerService.search(searchFilter, userId, userRole);
    }

    @GetMapping(value = "{orderId}")
    public OrderDetailsDTO getOrderDetails(@PathVariable String orderId) throws ItemNotFoundException, GoGasException {
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

    @GetMapping(value = "{orderId}/export")
    public void exportUserOrderItems(HttpServletResponse response, @PathVariable String orderId) throws IOException, ItemNotFoundException, GoGasException {
        AttachmentDTO excelAttachment = orderManagerService.extractExcelReport(orderId);
        excelAttachment.writeToHttpResponse(response);
    }

    @PreAuthorize("hasRole('A') OR @authorizationService.isOrderTypeManager(#orderDTO.orderTypeId)")
    @PostMapping()
    public BasicResponseDTO create(@RequestBody @Valid OrderDTO orderDTO) throws GoGasException {
        String orderId = orderManagerService.create(orderDTO).getId();
        // TODO creare nuovo anno contabile al primo ordine dell'anno
        return new BasicResponseDTO(orderId);
    }

    @PutMapping(value = "{orderId}")
    public BasicResponseDTO update(@PathVariable String orderId, @RequestBody @Valid OrderDTO orderDTO) throws ItemNotFoundException {
        String updatedOrderId = orderManagerService.update(orderId, orderDTO).getId();
        return new BasicResponseDTO(updatedOrderId);
    }

    @DeleteMapping(value = "{orderId}")
    public BasicResponseDTO delete(@PathVariable String orderId) {
        orderManagerService.delete(orderId);
        return new BasicResponseDTO("OK");
    }

    @PostMapping(value = "{orderId}/action/{actionCode}")
    public BasicResponseDTO changeStatus(@PathVariable String orderId, @PathVariable String actionCode,
                                         @RequestParam(required = false, defaultValue = "0") int roundType) throws ItemNotFoundException, InvalidOrderActionException {

        orderManagerService.changeStatus(orderId, actionCode, roundType);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/item/{itemId}")
    public BasicResponseDTO updateQty(@PathVariable String orderId, @PathVariable String itemId, @RequestBody @PositiveOrZero BigDecimal qty) throws ItemNotFoundException {
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
    public BasicResponseDTO updateAmountByUser(@PathVariable String orderId, @PathVariable String userId, @RequestBody  @PositiveOrZero BigDecimal cost) throws ItemNotFoundException {
        String accountingEntryId = orderManagerService.updateUserCost(orderId, userId, cost);
        return new BasicResponseDTO(accountingEntryId);
    }

    @DeleteMapping(value = "{orderId}/byUser/{userId}")
    public BasicResponseDTO deleteEntryByUser(@PathVariable String orderId, @PathVariable String userId) {
        orderManagerService.deleteUserCost(orderId, userId);
        return new BasicResponseDTO("OK");

    }

    @PostMapping(value = "{orderId}/shippingCost")
    public List<OrderByUserDTO> updateShippingCost(@PathVariable String orderId, @RequestBody  @PositiveOrZero BigDecimal cost) throws ItemNotFoundException {
        return orderManagerService.updateShippingCost(orderId, cost);
    }

    @PostMapping(value = "{orderId}/invoice/data")
    public BasicResponseDTO updateInvoiceData(@PathVariable String orderId, @RequestBody OrderInvoiceDataDTO invoiceData) throws GoGasException {
        orderManagerService.updateInvoiceData(orderId, invoiceData);
        return new BasicResponseDTO("OK");
    }

    @PostMapping(value = "{orderId}/invoice/attachment")
    public BasicResponseDTO uploadInvoiceAttachment(@PathVariable String orderId, @RequestParam("file") MultipartFile attachment) throws IOException, GoGasException {
        //TODO: filter only allowed content types
        byte[] invoiceFileContent = IOUtils.toByteArray(attachment.getInputStream());
        orderManagerService.saveInvoiceAttachment(orderId, invoiceFileContent, attachment.getContentType());
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "{orderId}/invoice/attachment")
    public void downloadInvoiceAttachment(HttpServletResponse response, @PathVariable String orderId) throws IOException, GoGasException {
        AttachmentDTO invoiceAttachment = orderManagerService.readInvoiceAttachment(orderId);
        invoiceAttachment.writeToHttpResponse(response);
    }

    @IsManager
    @GetMapping(value = "aequos/available")
    public List<OrderDTO> getAequosAvailableOpenOrders() {
        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderManagerService.getAvailableOrdersNotYetOpened(userId, userRole);
    }

    @PostMapping(value = "{orderId}/aequos/order/send")
    public BasicResponseDTO sendOrderToAequos(@PathVariable String orderId) throws GoGasException {
        String userId = authorizationService.getCurrentUser().getId();
        String aequosOrderId = orderManagerService.sendOrderToAequos(orderId, userId);
        return new BasicResponseDTO(aequosOrderId);
    }

    @PostMapping(value = "{orderId}/aequos/order/sendmail")
    public BasicResponseDTO sendOrderToAequosMail(@PathVariable String orderId) throws GoGasException {
        String userId = authorizationService.getCurrentUser().getId();
        orderManagerService.sendOrderToAequosMail(orderId, userId);
        return new BasicResponseDTO("OK");
    }

    @PostMapping(value = "{orderId}/aequos/order/synch")
    public BasicResponseDTO synchOrderFromAequos(@PathVariable String orderId) throws GoGasException {
        orderManagerService.synchOrderWithAequos(orderId, false);
        return new BasicResponseDTO("OK");
    }

    @PostMapping(value = "{orderId}/aequos/order/weights")
    public BasicResponseDTO sendWeigthsToAequos(@PathVariable String orderId) throws GoGasException {
        int updatedItems = orderManagerService.sendWeightsToAequos(orderId);
        return new BasicResponseDTO(updatedItems);
    }
    /********************************/

    @PutMapping(value = "{orderId}/product/{productId}/supplier")
    public BasicResponseDTO updateSupplierOrderQty(@PathVariable String orderId, @PathVariable String productId, @RequestBody  @PositiveOrZero int boxes) {
        orderManagerService.updateSupplierOrderQty(orderId, productId, boxes);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/cancel")
    public BasicResponseDTO cancelProductOrder(@PathVariable String orderId, @PathVariable String productId) {
        orderManagerService.cancelProductOrder(orderId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/product/{productId}/restore")
    public BasicResponseDTO restoreProductOrder(@PathVariable String orderId, @PathVariable String productId) {
        orderItemService.restoreProductOrder(orderId, productId);
        return new BasicResponseDTO("OK");
    }

    @PutMapping(value = "{orderId}/item/{orderItemId}/cancel")
    public BasicResponseDTO cancelOrderItem(@PathVariable String orderId, @PathVariable String orderItemId) {
        orderItemService.cancelOrderItem(orderItemId, orderId);
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
    public BasicResponseDTO updateProductPrice(@PathVariable String orderId, @PathVariable String productId, @RequestBody  @PositiveOrZero BigDecimal price) throws GoGasException {
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
