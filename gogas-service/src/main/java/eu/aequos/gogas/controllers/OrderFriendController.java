package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsOrderItemOwner;
import eu.aequos.gogas.service.OrderFriendService;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Api("Friends order fill")
@RestController
@RequestMapping("api/order/friend")
public class OrderFriendController {

    private OrderFriendService orderFriendService;
    private AuthorizationService authorizationService;

    public OrderFriendController(OrderFriendService orderFriendService,
                                 AuthorizationService authorizationService) {

        this.orderFriendService = orderFriendService;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "{orderId}/items", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserOrderItemDTO> getFriendsOrderItems(@PathVariable String orderId) throws ItemNotFoundException {
        String userId = authorizationService.getCurrentUser().getId();
        return orderFriendService.getOriginalFriendsOrder(userId, orderId);
    }

    @GetMapping(value = "{orderId}/product/{productId}")
    public List<OrderItemByProductDTO> getProductDetails(@PathVariable String orderId, @PathVariable String productId) throws ItemNotFoundException {
        String userId = authorizationService.getCurrentUser().getId();
        return orderFriendService.getFriendOrderItemsByProduct(userId, orderId, productId);
    }

    @PutMapping(value = "{orderId}/product/{productId}/accounted")
    public BasicResponseDTO setAccounted(@PathVariable String orderId, @PathVariable String productId, @RequestBody boolean accounted) throws ItemNotFoundException, GoGasException {
        String userId = authorizationService.getCurrentUser().getId();
        orderFriendService.setFriendAccounted(userId, orderId, productId, accounted);
        return new BasicResponseDTO("OK");
    }

    @IsOrderItemOwner
    @PutMapping(value = "{orderId}/product/{productId}/item/{itemId}")
    public OrderItemByProductDTO updateQty(@PathVariable String orderId, @PathVariable String productId, @PathVariable String itemId, @RequestBody BigDecimal qty) throws ItemNotFoundException, GoGasException {
        String userId = authorizationService.getCurrentUser().getId();
        return orderFriendService.updateFriendDeliveredQty(userId, orderId, productId, itemId, qty);
    }

    @GetMapping(value = "{orderId}/product/{productId}/availableUsers")
    public List<SelectItemDTO> getUsersNotOrdering(@PathVariable String orderId, @PathVariable String productId) throws ItemNotFoundException {
        String userId = authorizationService.getCurrentUser().getId();
        return orderFriendService.getFriendsNotOrdering(userId, orderId, productId);
    }

    @PostMapping(value = "{orderId}/item")
    public OrderItemByProductDTO insertOrderItem(@PathVariable String orderId, @RequestBody OrderItemUpdateRequest orderItem) throws GoGasException {
        if (!authorizationService.isFriend(orderItem.getUserId()))
            throw new UserNotAuthorizedException();

        String userId = authorizationService.getCurrentUser().getId();
        return orderFriendService.insertFriendOrderItem(userId, orderId, orderItem);
    }
}
