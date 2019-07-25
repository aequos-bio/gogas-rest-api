package eu.aequos.gogas.converter;

import eu.aequos.gogas.dto.SelectItemDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SelectItemsConverter {

    public <T> List<SelectItemDTO> toSelectItems(Stream<T> originalItems, Function<T, SelectItemDTO> convertion,
                                                 boolean addEmptySelection, String emptySelectionLabel) {

        Stream<SelectItemDTO> selectItems = originalItems.map(convertion);

        if (addEmptySelection)
            selectItems = Stream.concat(Stream.of(SelectItemDTO.empty(emptySelectionLabel)), selectItems);

        return selectItems.collect(Collectors.toList());
    }
}
