package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.service.OrderTypeService;
import eu.aequos.gogas.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("ordertype")
public class OrderTypeController {

    private OrderTypeService orderTypeService;
    private OrderManagerRepo orderManagerRepo;
    private UserService userService;

    public OrderTypeController(OrderTypeService orderTypeService,
                               OrderManagerRepo orderManagerRepo,
                               UserService userService) {

        this.orderTypeService = orderTypeService;
        this.orderManagerRepo = orderManagerRepo;
        this.userService = userService;
    }

    @GetMapping(value = "select")
    public List<SelectItemDTO> listOrderTypes(@RequestParam boolean firstEmpty,
                                              @RequestParam boolean referenteOnly,
                                              @RequestParam(required = false) boolean extended) {

        //TODO: filtrare visibili per referenti
        return orderTypeService.getAllAsSelectItems(extended, firstEmpty);
    }

    @GetMapping(value = "list")
    public List<OrderTypeDTO> listOrderTypes() {
        return orderTypeService.getAll();
    }

    @PostMapping()
    public String create(@RequestBody OrderTypeDTO orderTypeDTO) {
        return orderTypeService.create(orderTypeDTO).getId();
    }

    @PutMapping(value = "{orderTypeId}")
    public String update(@PathVariable String orderTypeId, @RequestBody OrderTypeDTO orderTypeDTO) throws ItemNotFoundException {
        return orderTypeService.update(orderTypeId, orderTypeDTO).getId();
    }

    @DeleteMapping(value = "{orderTypeId}")
    public BasicResponseDTO delete(@PathVariable String orderTypeId) {
        orderTypeService.delete(orderTypeId);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "{orderTypeId}/manager")
    public List<SelectItemDTO> listManagers(@PathVariable String orderTypeId) {
        List<OrderManager> orderManagerList = orderManagerRepo.findByOrderType(orderTypeId);
        Map<String, String> usersMap = userService.getUsersFullNameMap(orderManagerList.stream()
                .map(OrderManager::getUser)
                .collect(Collectors.toSet()));

        return orderManagerList.stream()
                .map(c -> new SelectItemDTO(c.getId(), usersMap.get(c.getUser())))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "{orderTypeId}/manager/available")
    public List<SelectItemDTO> listNotManagers(@PathVariable String orderTypeId) {
        Set<String> managers = orderManagerRepo.findByOrderType(orderTypeId).stream()
                .map(OrderManager::getUser)
                .collect(Collectors.toSet());

        Set<String> roles = userService.getAllUserRolesAsString(true, false);

        return userService.getActiveUsersForSelectByBlackListAndRoles(managers, roles);
    }

    @PutMapping(value = "{orderTypeId}/manager/{userId}")
    public String createManager(@PathVariable String orderTypeId, @PathVariable String userId) {
        OrderManager orderManager = new OrderManager();
        orderManager.setOrderType((orderTypeId));
        orderManager.setUser(userId);

        return orderManagerRepo.save(orderManager).getId();
    }

    @DeleteMapping(value = "manager/{orderManagerId}")
    public BasicResponseDTO deleteManager(@PathVariable String orderManagerId) {
        orderManagerRepo.deleteById(orderManagerId);
        return new BasicResponseDTO("OK");
    }
}
