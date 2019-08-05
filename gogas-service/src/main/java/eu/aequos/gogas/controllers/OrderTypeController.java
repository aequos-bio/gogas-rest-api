package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.OrderTypeSelectItemDTO;
import eu.aequos.gogas.persistence.entity.OrderType;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.repository.OrderTypeRepo;
import eu.aequos.gogas.converter.SelectItemsConverter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@RestController
@RequestMapping("ordertype")
public class OrderTypeController {

    private static final String EMPTY_SELECTION_LABEL = "Selezionare una tipologia...";
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_CONVERSION = t -> new SelectItemDTO(t.getId(), t.getDescription());
    private static final Function<OrderType, SelectItemDTO> SELECT_ITEM_EXTERNDED_CONVERSION = t ->
            new OrderTypeSelectItemDTO(t.getId(), t.getDescription(), t.getAequosOrderId(), t.isExternal(), t.getExternallink());

    private OrderTypeRepo orderTypeRepo;
    private SelectItemsConverter selectItemsConverter;

    public OrderTypeController(OrderTypeRepo orderTypeRepo, SelectItemsConverter selectItemsConverter) {
        this.orderTypeRepo = orderTypeRepo;
        this.selectItemsConverter = selectItemsConverter;
    }

    @GetMapping(value = "list")
    public List<SelectItemDTO> listOrderTypes(@RequestParam boolean firstEmpty,
                                              @RequestParam boolean referenteOnly,
                                              @RequestParam(required = false) boolean extended) {

        Stream<OrderType> orderTypeStream = orderTypeRepo.findAllByOrderByDescription().stream();
        Function<OrderType, SelectItemDTO> conversionFunc = extended ? SELECT_ITEM_EXTERNDED_CONVERSION : SELECT_ITEM_CONVERSION;
        return selectItemsConverter.toSelectItems(orderTypeStream, conversionFunc, firstEmpty, EMPTY_SELECTION_LABEL);
    }
}
