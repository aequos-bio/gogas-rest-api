package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.OrderDTO;
import eu.aequos.gogas.dto.filter.OrderSearchFilter;
import eu.aequos.gogas.exception.InvalidOrderActionException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.exception.OrderAlreadyExistsException;
import eu.aequos.gogas.exception.UserNotAuthorizedException;
import eu.aequos.gogas.service.ExcelGenerationService;
import eu.aequos.gogas.service.OrderManagerService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("order/manage")
public class OrderManagerController {

    private ExcelGenerationService reportService;
    private OrderManagerService orderManagerService;

    public OrderManagerController(ExcelGenerationService reportService, OrderManagerService orderManagerService) {
        this.reportService = reportService;
        this.orderManagerService = orderManagerService;
    }

    @PostMapping(value = "list")
    public List<OrderDTO> listOrders(@RequestBody OrderSearchFilter searchFilter) {
        return orderManagerService.search(searchFilter, "00000000-0000-0000-0000-000000000000"); //TODO: set user in session
    }


    @GetMapping(value = "{orderId}/export")
    public void getUserOrderItems(HttpServletResponse response, @PathVariable String orderId) throws IOException, ItemNotFoundException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.getOutputStream().write(reportService.extractOrderDetails(orderId));
        response.getOutputStream().flush();
    }

    @PostMapping()
    public String create(@RequestBody OrderDTO orderDTO) throws OrderAlreadyExistsException {
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
                         @RequestParam(required = false, defaultValue = "0") int roundType) throws ItemNotFoundException, UserNotAuthorizedException, InvalidOrderActionException {

        orderManagerService.changeStatus("00000000-0000-0000-0000-000000000000", orderId, actionCode, roundType);
        return "OK";
    }
}
