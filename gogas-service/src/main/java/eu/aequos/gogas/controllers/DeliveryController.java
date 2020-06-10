package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.delivery.DeliveryOrderDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.security.annotations.IsOrderManager;
import eu.aequos.gogas.service.DeliveryService;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Api("Order delivery operations")
@RestController
@RequestMapping("api/delivery")
@IsOrderManager
public class DeliveryController {

    private DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping(value = "{orderId}")
    public DeliveryOrderDTO getOrderDetailsForDelivery(@PathVariable String orderId) {
        return deliveryService.getOrderForDelivery(orderId);
    }

    @PostMapping(value = "{orderId}")
    public BasicResponseDTO updateQuantityFromDelivered(@PathVariable String orderId, @RequestBody DeliveryOrderDTO deliveredOrder) throws GoGasException {
        deliveryService.updateQuantityFromDelivered(orderId, deliveredOrder);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "{orderId}/file")
    public void downloadDeliveryFile(HttpServletResponse response, @PathVariable String orderId) throws IOException, GoGasException {
        AttachmentDTO deliveryAttachment = deliveryService.getOrderForDeliveryAsAttachment(orderId);
        deliveryAttachment.writeToHttpResponse(response);
    }

    @PostMapping(value = "{orderId}/file")
    public BasicResponseDTO uploadInvoiceAttachment(@PathVariable String orderId, @RequestParam("file") MultipartFile attachment) throws IOException, GoGasException {
        byte[] deliveryFileContent = IOUtils.toByteArray(attachment.getInputStream());
        deliveryService.updateQuantityFromFile(orderId, deliveryFileContent);
        return new BasicResponseDTO("OK");
    }
}
