package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.security.annotations.IsOrderManager;
import eu.aequos.gogas.service.DeliveryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/delivery")
@IsOrderManager
public class DeliveryController {

    private DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @IsOrderManager
    @GetMapping(value = "{orderId}")
    public DeliveryOrderDTO getOrderDetailsForDelivery(@PathVariable String orderId) {
        return deliveryService.getOrderForDelivery(orderId);
    }

    @IsOrderManager
    @PostMapping(value = "{orderId}")
    public BasicResponseDTO updateQuantityFromDelivered(@PathVariable String orderId, @RequestBody DeliveryOrderDTO deliveredOrder) throws GoGasException {
        deliveryService.updateQuantityFromDelivered(orderId, deliveredOrder);
        return new BasicResponseDTO("OK");
    }
}
