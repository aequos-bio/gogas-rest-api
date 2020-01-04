package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.OrderSynchroInfoDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsManager;
import eu.aequos.gogas.service.OrderTypeService;
import eu.aequos.gogas.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/ordertype")
public class OrderTypeController {

    private OrderTypeService orderTypeService;
    private OrderManagerRepo orderManagerRepo;
    private UserService userService;
    private AequosIntegrationService aequosIntegrationService;
    private AuthorizationService authorizationService;

    public OrderTypeController(OrderTypeService orderTypeService, OrderManagerRepo orderManagerRepo,
                               UserService userService, AequosIntegrationService aequosIntegrationService,
                               AuthorizationService authorizationService) {

        this.orderTypeService = orderTypeService;
        this.orderManagerRepo = orderManagerRepo;
        this.userService = userService;
        this.aequosIntegrationService = aequosIntegrationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "select")
    public List<SelectItemDTO> listOrderTypes(@RequestParam boolean firstEmpty,
                                              @RequestParam(required = false) boolean extended) {

        return orderTypeService.getAllAsSelectItems(extended, firstEmpty);
    }

    @IsManager
    @GetMapping(value = "select/manager")
    public List<SelectItemDTO> listManagerOrderTypes(@RequestParam boolean firstEmpty,
                                                     @RequestParam(required = false) boolean extended) {

        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderTypeService.getManagedAsSelectItems(extended, firstEmpty, userId, userRole);

    }

    @GetMapping(value = "list")
    public List<OrderTypeDTO> listOrderTypes() {
        return orderTypeService.getAll();
    }

    @IsAdmin
    @PutMapping(value = "aequos/sync")
    public BasicResponseDTO synchronizeWithAequos() {
        aequosIntegrationService.synchronizeOrderTypes();
        return new BasicResponseDTO("OK");
    }

    @IsAdmin
    @PostMapping()
    public String create(@RequestBody OrderTypeDTO orderTypeDTO) {
        return orderTypeService.create(orderTypeDTO).getId();
    }

    @IsAdmin
    @PutMapping(value = "{orderTypeId}")
    public String update(@PathVariable String orderTypeId, @RequestBody OrderTypeDTO orderTypeDTO) throws ItemNotFoundException {
        return orderTypeService.update(orderTypeId, orderTypeDTO).getId();
    }

    @IsAdmin
    @DeleteMapping(value = "{orderTypeId}")
    public BasicResponseDTO delete(@PathVariable String orderTypeId) {
        orderTypeService.delete(orderTypeId);
        return new BasicResponseDTO("OK");
    }

    @GetMapping(value = "{orderTypeId}/synchro/info")
    public OrderSynchroInfoDTO getSynchroInfo(@PathVariable String orderTypeId) throws ItemNotFoundException {
        OrderType orderType = orderTypeService.getRequired(orderTypeId);

        if (orderType.getAequosOrderId() == null)
            return new OrderSynchroInfoDTO( null);
        else
            return new OrderSynchroInfoDTO(orderType.getLastsynchro())
                    .withAequosOrderId(orderType.getAequosOrderId());
    }

    @IsAdmin
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

    @IsAdmin
    @GetMapping(value = "{orderTypeId}/manager/available")
    public List<SelectItemDTO> listNotManagers(@PathVariable String orderTypeId) {
        Set<String> managers = orderManagerRepo.findByOrderType(orderTypeId).stream()
                .map(OrderManager::getUser)
                .collect(Collectors.toSet());

        Set<String> roles = userService.getAllUserRolesAsString(true, false);

        return userService.getActiveUsersForSelectByBlackListAndRoles(managers, roles);
    }

    @IsAdmin
    @PutMapping(value = "{orderTypeId}/manager/{userId}")
    public String createManager(@PathVariable String orderTypeId, @PathVariable String userId) {
        OrderManager orderManager = new OrderManager();
        orderManager.setOrderType((orderTypeId));
        orderManager.setUser(userId);

        return orderManagerRepo.save(orderManager).getId();
    }

    @IsAdmin
    @DeleteMapping(value = "manager/{orderManagerId}")
    public BasicResponseDTO deleteManager(@PathVariable String orderManagerId) {
        orderManagerRepo.deleteById(orderManagerId);
        return new BasicResponseDTO("OK");
    }
}
