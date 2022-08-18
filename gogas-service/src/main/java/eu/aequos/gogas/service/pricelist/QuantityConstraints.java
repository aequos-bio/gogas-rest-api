package eu.aequos.gogas.service.pricelist;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuantityConstraints {
    private final boolean wholeBoxesOnly;
    private final BigDecimal multiple;
}
