package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.persistence.entity.derived.ProductTotalOrder;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.GoGasUserDetails;
import eu.aequos.gogas.service.OrderUserService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Api("User order fill")
@RestController
@RequestMapping("api/order/user")
@Validated
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderUserService orderUserService;
    private final AuthorizationService authorizationService;

    @GetMapping(value = "open")
    public List<OpenOrderDTO> getOpenOrders() {
        return orderUserService.getOpenOrders(authorizationService.getCurrentUser().getId());
    }

    @PostMapping(value = "list")
    public List<OrderDTO> listOrders(@RequestBody OrderSearchFilter searchFilter) {
        GoGasUserDetails currentUser = authorizationService.getCurrentUser();
        return orderUserService.search(searchFilter, currentUser.getId(), currentUser.getRole());
    }

    @GetMapping(value = "{orderId}")
    public UserOrderDetailsDTO getOrderDetails(@PathVariable String orderId, @RequestParam(required = false, defaultValue = "false") boolean includeTotalAmount) throws GoGasException {
        GoGasUserDetails currentUser = authorizationService.getCurrentUser();
        return orderUserService.getOrderDetails(currentUser.getId(), orderId, includeTotalAmount);
    }

    @GetMapping(value = "{orderId}/categories")
    public List<CategoryDTO> getOrderCategories(@PathVariable String orderId) throws GoGasException {
        return orderUserService.getOrderCategories(orderId);
    }

    @GetMapping(value = "{orderId}/categories/{categoryId}/not-ordered", produces = MediaType.APPLICATION_JSON_VALUE)
    public CategoryDTO getNotOrderedItemsByCategory(@PathVariable String orderId, @PathVariable String categoryId) throws GoGasException {
        GoGasUserDetails currentUser = authorizationService.getCurrentUser();
        return orderUserService.getNotOrderedItemsByCategory(currentUser.getId(), orderId, categoryId);
    }

    @GetMapping(value = "{orderId}/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserOrderItemDTO> getUserOrderItems(@PathVariable String orderId, @RequestParam String userId) throws GoGasException {
        if (!authorizationService.isUserOrFriend(userId))
            throw new UserNotAuthorizedException();

        return orderUserService.getUserOrderItems(orderId, userId);
    }

    @PostMapping(value = "{orderId}/item", produces = MediaType.APPLICATION_JSON_VALUE)
    public SmallUserOrderItemDTO updateUserOrder(@PathVariable String orderId, @RequestBody @Valid OrderItemUpdateRequest orderItemUpdate) throws GoGasException {

        if (!authorizationService.isUserOrFriend(orderItemUpdate.getUserId()))
            throw new UserNotAuthorizedException();

        return orderUserService.updateUserOrder(orderId, orderItemUpdate);
    }

    @GetMapping(value = "{orderId}/product/{productId}/total")
    public Optional<ProductTotalOrder> getProductTotalQuantity(@PathVariable String orderId, @PathVariable String productId) throws GoGasException {
        return orderUserService.getTotalQuantityByProduct(orderId, productId);
    }

    @GetMapping(value = "{orderId}/product/total")
    public List<ProductTotalOrder> getProductsTotalQuantity(@PathVariable String orderId) throws GoGasException {
        return orderUserService.getTotalQuantityByProduct(orderId);
    }
}
