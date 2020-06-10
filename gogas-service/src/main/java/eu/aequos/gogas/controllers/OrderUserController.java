package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.service.OrderUserService;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("User order fill")
@RestController
@RequestMapping("api/order/user")
public class OrderUserController {

    private OrderUserService orderUserService;
    private AuthorizationService authorizationService;

    public OrderUserController(OrderUserService orderUserService,
                               AuthorizationService authorizationService) {

        this.orderUserService = orderUserService;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "open")
    public List<OpenOrderDTO> getOpenOrders() {
        return orderUserService.getOpenOrders(authorizationService.getCurrentUser().getId());
    }

    @PostMapping(value = "list")
    public List<OrderDTO> listOrders(@RequestBody OrderSearchFilter searchFilter) {
        return orderUserService.search(searchFilter, authorizationService.getCurrentUser().getId());
    }

    @GetMapping(value = "{orderId}")
    public UserOrderDetailsDTO getOrderDetails(@PathVariable String orderId) throws GoGasException {
        return orderUserService.getOrderDetails(orderId);
    }

    @GetMapping(value = "{orderId}/items", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserOrderItemDTO> getUserOrderItems(@PathVariable String orderId, @RequestParam String userId) throws GoGasException {
        if (!authorizationService.isUserOrFriend(userId))
            throw new UserNotAuthorizedException();

        return orderUserService.getUserOrderItems(orderId, userId);
    }

    @PostMapping(value = "{orderId}/item", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SmallUserOrderItemDTO updateUserOrder(@PathVariable String orderId, @RequestBody OrderItemUpdateRequest orderItemUpdate) throws GoGasException {

        if (!authorizationService.isUserOrFriend(orderItemUpdate.getUserId()))
            throw new UserNotAuthorizedException();

        return orderUserService.updateUserOrder(orderId, orderItemUpdate);
    }
}
