package eu.aequos.gogas.persistence.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SpecificationBuilder<T> {
    Specification<T> filter;

    public SpecificationBuilder<T> withBaseFilter(Specification<T> baseFilter) {
        filter = baseFilter;
        return this;
    }

    public <U> SpecificationBuilder<T> and(Function<U, Specification<T>> func, U value) {
        filter = applyFilterIfNotNull(filter, func, Specification::and, value);
        return this;
    }

    public <U> SpecificationBuilder<T> or(Function<U, Specification<T>> func, U value) {
        filter = applyFilterIfNotNull(filter, func, Specification::or, value);
        return this;
    }

    private <U> Specification<T> applyFilterIfNotNull(Specification<T> spec,
                                                      Function<U, Specification<T>> func,
                                                      BiFunction<Specification<T>, Specification<T>, Specification<T>> operator,
                                                      U value) {
        if (value == null)
            return spec;

        return operator.apply(spec, func.apply(value));
    }

    public Specification<T> build() {
        return filter;
    }
}
