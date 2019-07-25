package eu.aequos.gogas.controllers;

import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.service.ExcelGenerationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("order/manage")
public class OrderManagerController {

    private ExcelGenerationService reportService;

    public OrderManagerController(ExcelGenerationService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "{orderId}/export")
    public void getUserOrderItems(HttpServletResponse response, @PathVariable String orderId) throws IOException, ItemNotFoundException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.getOutputStream().write(reportService.extractOrderDetails(orderId));
        response.getOutputStream().flush();
    }
}
