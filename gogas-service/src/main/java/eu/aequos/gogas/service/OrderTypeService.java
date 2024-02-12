package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.OrderTypeAccountingDTO;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.OrderTypeSelectItemDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.integration.AequosIntegrationService;
import eu.aequos.gogas.persistence.entity.OrderManager;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.entity.OrderUserBlacklist;
import eu.aequos.gogas.persistence.entity.User;
import eu.aequos.gogas.persistence.repository.OrderManagerRepo;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import eu.aequos.gogas.persistence.repository.OrderUserBlacklistRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Service
public class OrderTypeService extends CrudService<OrderType, String> {

    private static final String EMPTY_SELECTION_LABEL = "Selezionare una tipologia...";
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_CONVERSION = t -> new SelectItemDTO(t.getId(), t.getDescription());
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_EXTENDED_CONVERSION = t ->
            new OrderTypeSelectItemDTO(t.getId(), t.getDescription(), t.getAequosOrderId(), t.isExternal(), t.getExternallink());

    private final OrderTypeRepo orderTypeRepo;
    private final OrderRepo orderRepo;
    private final OrderManagerRepo orderManagerRepo;
    private final AequosIntegrationService aequosIntegrationService;
    private final OrderUserBlacklistRepo orderUserBlacklistRepo;

    @Override
    protected CrudRepository<OrderType, String> getCrudRepository() {
        return orderTypeRepo;
    }

    @Override
    protected String getType() {
        return "order type";
    }

    public OrderTypeDTO getById(String orderTypeId) {
        OrderType orderType = getRequired(orderTypeId);
        Set<String> usedOrderTypes = orderRepo.findAllUsedOrderTypes();
        return new OrderTypeDTO().fromModel(orderType, usedOrderTypes.contains(orderTypeId));
    }

    public List<OrderTypeDTO> getAll() {
        Set<String> usedOrderTypes = orderRepo.findAllUsedOrderTypes();

        return orderTypeRepo.findAllByOrderByDescription().stream()
                .map(type -> new OrderTypeDTO().fromModel(type, usedOrderTypes.contains(type.getId())))
                .collect(Collectors.toList());
    }

    public Map<String, OrderType> getAllOrderTypesMapping() {
        return StreamSupport.stream(orderTypeRepo.findAll().spliterator(), false)
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
        setAccountingCode(newAequosOrderTypes);
        orderTypeRepo.saveAll(newAequosOrderTypes);
    }

    private void setAccountingCode(List<OrderType> newAequosOrderTypes) {
        String aequosAccountingCode = orderTypeRepo.findAequosAccountingCode();

        if (aequosAccountingCode == null) {
            return;
        }

        newAequosOrderTypes.stream()
                .filter(OrderType::isBilledByAequos)
                .forEach(orderType -> orderType.setAccountingCode(aequosAccountingCode));
    }

    public List<SelectItemDTO> getAllAsSelectItems(boolean extended, boolean firstEmpty) {
        List<OrderType> orderTypeStream = orderTypeRepo.findAllByOrderByDescription();
        return convertToSelectItems(extended, firstEmpty, orderTypeStream);
    }

    public List<SelectItemDTO> getBlacklistAsSelectItems(String userId) {
        List<String> blacklistOrderIds = orderUserBlacklistRepo.getOrderIdsByUserId(userId);
        List<OrderType> orderTypeStream = orderTypeRepo.findByIdInOrderByDescription(blacklistOrderIds);
        return convertToSelectItems(false, false, orderTypeStream);
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

    @Transactional
    public void updateUserBlacklist(String userId, List<String> orderTypeIds) {
        orderUserBlacklistRepo.deleteByUserId(userId);

        if (orderTypeIds.isEmpty()) {
            return;
        }

        List<OrderUserBlacklist> orderUserBlacklists = orderTypeIds.stream()
                .map(orderTypeId -> {
                    OrderUserBlacklist orderUserBlacklist = new OrderUserBlacklist();
                    orderUserBlacklist.setOrderTypeId(orderTypeId);
                    orderUserBlacklist.setUserId(userId);
                    return orderUserBlacklist;
                })
                .collect(toList());


        orderUserBlacklistRepo.saveAll(orderUserBlacklists);
    }

    public void deleteFromBlacklist(String orderTypeId, String userId) {
        orderUserBlacklistRepo.deleteById(new OrderUserBlacklist.Key(orderTypeId, userId));
    }
}
