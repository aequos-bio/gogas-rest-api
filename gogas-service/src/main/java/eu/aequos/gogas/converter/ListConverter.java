package eu.aequos.gogas.converter;

import eu.aequos.gogas.dto.SelectItemDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListConverter<T> {

    private Stream<T> items;

    protected ListConverter(Stream<T> items) {
        this.items = items;
    }

    public static <T> ListConverter<T> fromStream(Stream<T> items) {
        return new ListConverter<>(items);
    }

    public static <T> ListConverter<T> fromList(Collection<T> itemsList) {
        return new ListConverter<>(itemsList.stream());
    }

    public List<SelectItemDTO> toSelectItems(Function<T, SelectItemDTO> conversion,
                                                 boolean addEmptySelection, String emptySelectionLabel) {

        Stream<SelectItemDTO> selectItems = items.map(conversion);

        if (addEmptySelection)
            selectItems = Stream.concat(Stream.of(SelectItemDTO.empty(emptySelectionLabel)), selectItems);

        return selectItems.collect(Collectors.toList());
    }

    public Set<String> extractIds(Function<T, String> idGetter) {
        return items.map(idGetter)
                .collect(Collectors.toSet());
    }

    public Map<String, T> toMap(Function<T, String> keyGenerator) {
        return items.collect(Collectors.toMap(keyGenerator, Function.identity()));
    }
}