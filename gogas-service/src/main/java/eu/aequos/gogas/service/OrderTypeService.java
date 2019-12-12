package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.ListConverter;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.OrderTypeSelectItemDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderTypeService extends CrudService<OrderType, String> {

    private static final String EMPTY_SELECTION_LABEL = "Selezionare una tipologia...";
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_CONVERSION = t -> new SelectItemDTO(t.getId(), t.getDescription());
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_EXTENDED_CONVERSION = t ->
            new OrderTypeSelectItemDTO(t.getId(), t.getDescription(), t.getAequosOrderId(), t.isExternal(), t.getExternallink());

    private OrderTypeRepo orderTypeRepo;
    private UserService userService;
    private OrderRepo orderRepo;
    private ListConverter listConverter;

    public OrderTypeService(OrderTypeRepo orderTypeRepo, UserService userService,
                            OrderRepo orderRepo, ListConverter listConverter) {
        super(orderTypeRepo, "order type");

        this.orderTypeRepo = orderTypeRepo;
        this.userService = userService;
        this.orderRepo = orderRepo;
        this.listConverter = listConverter;
    }

    public List<OrderTypeDTO> getAll() {
        Set<String> usedOrderTypes = orderRepo.findAllUsedOrderTypes();

        return orderTypeRepo.findAllByOrderByDescription().stream()
                .map(type -> new OrderTypeDTO().fromModel(type, usedOrderTypes.contains(type.getId())))
                .collect(Collectors.toList());
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

    public void createAll(List<OrderType> orderTypesToBeCreated) {
        orderTypeRepo.saveAll(orderTypesToBeCreated);
    }

    public List<SelectItemDTO> getAllAsSelectItems(boolean extended, boolean firstEmpty) {
        Stream<OrderType> orderTypeStream = orderTypeRepo.findAllByOrderByDescription().stream();
        Function<OrderType, SelectItemDTO> conversionFunc = extended ? SELECT_ITEM_EXTENDED_CONVERSION : SELECT_ITEM_CONVERSION;
        return ListConverter.fromStream(orderTypeStream)
                .toSelectItems(conversionFunc, firstEmpty, EMPTY_SELECTION_LABEL);
    }
}
