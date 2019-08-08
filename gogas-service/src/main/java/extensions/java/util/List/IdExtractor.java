package extensions.java.util.List;

import manifold.ext.api.Extension;
import manifold.ext.api.Self;
import manifold.ext.api.This;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Extension
public class IdExtractor {

    public static <E> List<String> extractId(@This List<E> entities, Function<E, String> idGetter) {
        return entities.stream()
                .map(idGetter)
                .collect(Collectors.toList());
    }

    public static <E> Set<String> extractIds(@This List<E> entities, Function<E, String> idGetter) {
        return entities.stream()
                .map(idGetter)
                .collect(Collectors.toSet());
    }

    public static <E> Map<String, E> toMap(@This List<E> entities, Function<E, String> idGetter) {
        return entities.stream()
                .collect(Collectors.toMap(idGetter, Function.identity()));
    }
}
