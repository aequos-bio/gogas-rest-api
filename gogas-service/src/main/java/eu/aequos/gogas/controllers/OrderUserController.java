package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.UserOrderItemDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.UnknownOrderStatusException;
import eu.aequos.gogas.persistence.entity.Order;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.service.OrderUserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("order/user")
public class OrderUserController {

    private OrderUserService orderUserService;
    private OrderRepo orderRepo;

    public OrderUserController(OrderUserService orderUserService, OrderRepo orderRepo) {
        this.orderUserService = orderUserService;
        this.orderRepo = orderRepo;
    }

    @GetMapping(value = "{orderId}/items", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<UserOrderItemDTO> getUserOrderItems(@PathVariable String orderId, @RequestParam String userId) throws GoGasException, UnknownOrderStatusException {
        return orderUserService.getUserOrderItems(orderId, userId);
    }

    @GetMapping(value = "open", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<Order> getUserOrderItems(@RequestParam String userId) {
        return orderRepo.openOrders(userId);
    }
}
