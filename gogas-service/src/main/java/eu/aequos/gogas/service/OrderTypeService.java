package eu.aequos.gogas.service;

import eu.aequos.gogas.converter.SelectItemsConverter;
import eu.aequos.gogas.dto.OrderTypeDTO;
import eu.aequos.gogas.dto.OrderTypeSelectItemDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.persistence.repository.OrderRepo;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private SelectItemsConverter selectItemsConverter;

    public OrderTypeService(OrderTypeRepo orderTypeRepo, UserService userService,
                            OrderRepo orderRepo, SelectItemsConverter selectItemsConverter) {
        super(orderTypeRepo, "order type");

        this.orderTypeRepo = orderTypeRepo;
        this.userService = userService;
        this.orderRepo = orderRepo;
        this.selectItemsConverter = selectItemsConverter;
    }

    public List<OrderTypeDTO> getAll() {
        Set<String> usedOrderTypes = orderRepo.findAllUsedOrderTypes();

        return orderTypeRepo.findAllByOrderByDescription().stream()
                .map(type -> new OrderTypeDTO().fromModel(type, usedOrderTypes.contains(type.getId())))
                .collect(Collectors.toList());
    }

    public List<SelectItemDTO> getAllAsSelectItems(boolean extended, boolean firstEmpty) {
        Stream<OrderType> orderTypeStream = orderTypeRepo.findAllByOrderByDescription().stream();
        Function<OrderType, SelectItemDTO> conversionFunc = extended ? SELECT_ITEM_EXTENDED_CONVERSION : SELECT_ITEM_CONVERSION;
        return selectItemsConverter.toSelectItems(orderTypeStream, conversionFunc, firstEmpty, EMPTY_SELECTION_LABEL);
    }
}
