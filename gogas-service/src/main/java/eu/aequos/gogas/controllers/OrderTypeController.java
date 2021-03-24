package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.security.AuthorizationService;
import eu.aequos.gogas.security.annotations.IsAdmin;
import eu.aequos.gogas.security.annotations.IsManager;
import eu.aequos.gogas.service.OrderTypeService;
import eu.aequos.gogas.service.UserService;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Api("Order types")
@RestController
@RequestMapping("api/ordertype")
public class OrderTypeController {

    private OrderTypeService orderTypeService;
    private OrderManagerRepo orderManagerRepo;
    private UserService userService;
    private AuthorizationService authorizationService;

    public OrderTypeController(OrderTypeService orderTypeService, OrderManagerRepo orderManagerRepo,
                               UserService userService, AuthorizationService authorizationService) {

        this.orderTypeService = orderTypeService;
        this.orderManagerRepo = orderManagerRepo;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @ApiOperation(
        value = "List all for dropdown selection",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="any role", description = "any role") }) }
    )
    @GetMapping(value = "select")
    public List<SelectItemDTO> listOrderTypes(@ApiParam("Include first entry 'empty'") @RequestParam boolean firstEmpty,
                                              @ApiParam("Include additional fields") @RequestParam(required = false) boolean extended) {

        return orderTypeService.getAllAsSelectItems(extended, firstEmpty);
    }

    @ApiOperation(
        value = "List managed only for dropdown selection",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin"), @AuthorizationScope(scope ="order manager", description = "order manager") }) }
    )
    @IsManager
    @GetMapping(value = "select/manager")
    public List<SelectItemDTO> listManagerOrderTypes(@ApiParam("Include first entry 'empty'") @RequestParam boolean firstEmpty,
                                                     @ApiParam("Include additional fields") @RequestParam(required = false) boolean extended) {

        String userId = authorizationService.getCurrentUser().getId();
        User.Role userRole = User.Role.valueOf(authorizationService.getCurrentUser().getRole());
        return orderTypeService.getManagedAsSelectItems(extended, firstEmpty, userId, userRole);

    }

    @ApiOperation(
        value = "List order types",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="any role", description = "any role") }) }
    )
    @GetMapping(value = "list")
    public List<OrderTypeDTO> listOrderTypes() {
        return orderTypeService.getAll();
    }

    @ApiOperation(
        value = "Get order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = OrderTypeDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: orderType, Id: <orderTypeId>"),
    })
    @IsAdmin
    @GetMapping(value = "{orderTypeId}")
    public OrderTypeDTO getOrderType(@PathVariable String orderTypeId) {
        return orderTypeService.getById(orderTypeId);
    }

    @ApiOperation(
        value = "Synchronize order types with Aequos",
        notes = "Order types are retrieved from aequos service and created if not existing",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @PutMapping(value = "aequos/sync")
    public BasicResponseDTO synchronizeWithAequos() {
        orderTypeService.synchronizeAequosOrderTypes();
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Create order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @PostMapping()
    public BasicResponseDTO createOrderType(@RequestBody OrderTypeDTO orderTypeDTO) {
        String orderId = orderTypeService.create(orderTypeDTO).getId();
        return new BasicResponseDTO(orderId);
    }

    @ApiOperation(
        value = "Modify order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsAdmin
    @PutMapping(value = "{orderTypeId}")
    public BasicResponseDTO updateOrderType(@PathVariable String orderTypeId, @RequestBody OrderTypeDTO orderTypeDTO) throws ItemNotFoundException {
        String updatedOrderId = orderTypeService.update(orderTypeId, orderTypeDTO).getId();
        return new BasicResponseDTO(updatedOrderId);
    }

    @ApiOperation(
        value = "Delete order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>"),
        @ApiResponse(code = 409, message = "L'elemento non può essere eliminato")
    })
    @IsAdmin
    @DeleteMapping(value = "{orderTypeId}")
    public BasicResponseDTO deleteOrderType(@PathVariable String orderTypeId) {
        orderTypeService.delete(orderTypeId);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Get last synchronization info",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="?", description = "?") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = OrderSynchroInfoDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @GetMapping(value = "{orderTypeId}/synchro/info")
    public OrderSynchroInfoDTO getSynchroInfo(@PathVariable String orderTypeId) throws ItemNotFoundException {
        OrderType orderType = orderTypeService.getRequired(orderTypeId);

        if (orderType.getAequosOrderId() == null)
            return new OrderSynchroInfoDTO( null);
        else
            return new OrderSynchroInfoDTO(orderType.getLastsynchro())
                    .withAequosOrderId(orderType.getAequosOrderId());
    }

    @ApiOperation(
        value = "Get managers of order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = SelectItemDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
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

    @ApiOperation(
        value = "Get managers of all order types",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @GetMapping(value = "manager/list")
    public List<ManagerDTO> listManagers() {
        Iterable<OrderManager> orderManagerIterable = orderManagerRepo.findAll();
        List<OrderManager> orderManagerList = StreamSupport.stream(orderManagerIterable.spliterator(), false)
                .collect(Collectors.toList());
        Map<String, String> usersMap = userService.getUsersFullNameMap(orderManagerList.stream()
            .map(OrderManager::getUser)
            .collect(Collectors.toSet()));

        Map<String, OrderType> orderTypesMap = orderTypeService.getAllOrderTypesMapping();

        List<ManagerDTO> managers = new ArrayList<>();
        for(OrderManager orderManager : orderManagerList) {
            ManagerDTO manager = ManagerDTO.fromOrderManager(orderManager);
            manager.setUserName(usersMap.get(orderManager.getUser()));
            manager.setOrderTypeName(orderTypesMap.get(orderManager.getOrderType()).getDescription());
            managers.add(manager);
        }
        return managers;
    }

    @ApiOperation(
        value = "Get users who are NOT managers of order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = SelectItemDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsAdmin
    @GetMapping(value = "{orderTypeId}/manager/available")
    public List<SelectItemDTO> listNotManagers(@PathVariable String orderTypeId) {
        Set<String> managers = orderManagerRepo.findByOrderType(orderTypeId).stream()
                .map(OrderManager::getUser)
                .collect(Collectors.toSet());

        Set<String> roles = userService.getAllUserRolesAsString(true, false);

        if (managers.isEmpty())
            return userService.getActiveUsersByRoles(roles);

        return userService.getActiveUsersForSelectByBlackListAndRoles(managers, roles);
    }

    @ApiOperation(
        value = "Add a manager to order type",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @PutMapping(value = "{orderTypeId}/manager/{userId}")
    public BasicResponseDTO createManager(@PathVariable String orderTypeId, @PathVariable String userId) {
        OrderManager orderManager = new OrderManager();
        orderManager.setOrderType((orderTypeId));
        orderManager.setUser(userId);

        String orderManagerId = orderManagerRepo.save(orderManager).getId();
        return new BasicResponseDTO(orderManagerId);
    }

    @ApiOperation(
        value = "Remove a managers from order types",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @DeleteMapping(value = "manager/{orderManagerId}")
    public BasicResponseDTO deleteManager(@ApiParam("Id of the association") @PathVariable String orderManagerId) {
        orderManagerRepo.deleteById(orderManagerId);
        return new BasicResponseDTO("OK");
    }

    @ApiOperation(
        value = "Get list of accounting code for order types",
        notes = "Association of accounting code woth order type. For all the order types billed by Aequos, a unique special entry is returned with id 'aequos'.",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @IsAdmin
    @GetMapping(value = "accounting")
    public List<OrderTypeAccountingDTO> getOrderTypesForAccounting() {
        return orderTypeService.getForAccounting();
    }

    @ApiOperation(
        value = "Update accounting code for an order types",
        notes = "The special orderTypeId 'aequos' is used to update all the order types billed by Aequos.",
        authorizations = { @Authorization(value = "jwt", scopes = { @AuthorizationScope(scope ="admin", description = "admin") }) }
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = BasicResponseDTO.class),
        @ApiResponse(code = 404, message = "Item not found. Type: productType, Id: <productTypeId>")
    })
    @IsAdmin
    @PutMapping(value = "{orderTypeId}/accounting")
    public BasicResponseDTO updateAcccountingCode(@PathVariable String orderTypeId, @RequestBody Map<String, String> value) {
        orderTypeService.updateAccountingCode(orderTypeId, value.get("accountingCode"));
        return new BasicResponseDTO("OK");
    }
}
