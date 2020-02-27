package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.*;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Service
public class OrderTypeService extends CrudService<OrderType, String> {

    private static final String EMPTY_SELECTION_LABEL = "Selezionare una tipologia...";
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_CONVERSION = t -> new SelectItemDTO(t.getId(), t.getDescription());
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_EXTENDED_CONVERSION = t ->
            new OrderTypeSelectItemDTO(t.getId(), t.getDescription(), t.getAequosOrderId(), t.isExternal(), t.getExternallink());

    private OrderTypeRepo orderTypeRepo;
    private OrderRepo orderRepo;
    private OrderManagerRepo orderManagerRepo;
    private AequosIntegrationService aequosIntegrationService;

    public OrderTypeService(OrderTypeRepo orderTypeRepo, OrderRepo orderRepo,
                            OrderManagerRepo orderManagerRepo,
                            AequosIntegrationService aequosIntegrationService) {
        super(orderTypeRepo, "order type");

        this.orderTypeRepo = orderTypeRepo;
        this.orderRepo = orderRepo;
        this.orderManagerRepo = orderManagerRepo;
        this.aequosIntegrationService = aequosIntegrationService;
    }

    public List<OrderTypeDTO> getAll() {
        Set<String> usedOrderTypes = orderRepo.findAllUsedOrderTypes();

        return orderTypeRepo.findAllByOrderByDescription().stream()
                .map(type -> new OrderTypeDTO().fromModel(type, usedOrderTypes.contains(type.getId())))
                .collect(Collectors.toList());
    }

    public Map<String, OrderType> getAllOrderTypesMapping() {
        List<OrderType> list = StreamSupport.stream(orderTypeRepo.findAll().spliterator(), false)
            .collect(Collectors.toList());
        return list.stream()
            .collect(Collectors.toMap(OrderType::getId, Function.identity()));
    }

    public Set<Integer> getAequosOrderTypes() {
        return orderTypeRepo.findByAequosOrderIdNotNull().stream()
                .map(OrderType::getAequosOrderId)
                .collect(Collectors.toSet());
    }

    public Map<Integer, OrderType> getAequosOrderTypesMapping() {
        return orderTypeRepo.findByAequosOrderIdNotNull().stream()
                .collect(Collectors.toMap(OrderType::getAequosOrderId, Function.identity()));
    }

    public void synchronizeAequosOrderTypes() {
        Set<Integer> existingAequosOrderTypes = getAequosOrderTypes();
        List<OrderType> newAequosOrderTypes = aequosIntegrationService.synchronizeOrderTypes(existingAequosOrderTypes);
        orderTypeRepo.saveAll(newAequosOrderTypes);
    }

    public List<SelectItemDTO> getAllAsSelectItems(boolean extended, boolean firstEmpty) {
        List<OrderType> orderTypeStream = orderTypeRepo.findAllByOrderByDescription();
        return convertToSelectItems(extended, firstEmpty, orderTypeStream);
    }

    public List<SelectItemDTO> getManagedAsSelectItems(boolean extended, boolean firstEmpty, String userId, User.Role userRole) {
        if (userRole.isAdmin())
            return getAllAsSelectItems(extended, firstEmpty);

        List<String> managedOrderIds = orderManagerRepo.findByUser(userId).stream()
                .map(OrderManager::getOrderType)
                .collect(toList());

        List<OrderType> orderTypeStream = orderTypeRepo.findByIdInOrderByDescription(managedOrderIds);
        return convertToSelectItems(extended, firstEmpty, orderTypeStream);
    }

    private List<SelectItemDTO> convertToSelectItems(boolean extended, boolean firstEmpty, List<OrderType> orderTypeStream) {
        Function<OrderType, SelectItemDTO> conversionFunc = extended ? SELECT_ITEM_EXTENDED_CONVERSION : SELECT_ITEM_CONVERSION;
        return ListConverter.fromList(orderTypeStream)
                .toSelectItems(conversionFunc, firstEmpty, EMPTY_SELECTION_LABEL);
    }

    public List<OrderTypeAccountingDTO> getForAccounting() {
        String aequosUniqueAccountingCode = orderTypeRepo.findAequosAccountingCode();
        OrderTypeAccountingDTO aequosOrderType = new OrderTypeAccountingDTO("aequos", "Ordini Aequos", aequosUniqueAccountingCode);

        List<OrderTypeAccountingDTO> result = orderTypeRepo.findOrderTypesNotBilledByAequos().stream()
                .map(t -> new OrderTypeAccountingDTO(t.getId(), t.getDescription(), t.getAccountingCode()))
                .collect(toList());

        result.add(aequosOrderType);

        return result;
    }

    @Transactional
    public void updateAccountingCode(String orderTypeId, String accountingCode) {
        if (orderTypeId.equalsIgnoreCase("aequos")) {
            orderTypeRepo.updateAequosAccountingCode(accountingCode);
            return;
        }

        int updatedRows = orderTypeRepo.updateAccountingCode(orderTypeId, accountingCode);
        if (updatedRows < 1)
            throw new ItemNotFoundException("orderType", orderTypeId);
    }
}
