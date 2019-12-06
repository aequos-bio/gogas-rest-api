package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.OrderItemUpdateRequest;
import eu.aequos.gogas.dto.SmallUserOrderItemDTO;
import eu.aequos.gogas.dto.UserOrderItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.service.OrderUserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping(value = "{orderId}/items", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserOrderItemDTO> getUserOrderItems(@PathVariable String orderId, @RequestParam String userId) throws GoGasException {
        return orderUserService.getUserOrderItems(orderId, userId);
    }

    @PostMapping(value = "{orderId}/item", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SmallUserOrderItemDTO updateUserOrder(@PathVariable String orderId, @RequestBody OrderItemUpdateRequest orderItemUpdate) throws GoGasException {

        if (!authorizationService.isUserOrFriend(orderItemUpdate.getUserId()))
            throw new UserNotAuthorizedException();

        return orderUserService.updateUserOrder(orderId, orderItemUpdate);
    }
}
