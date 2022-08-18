package eu.aequos.gogas.service.pricelist;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExternalPriceListItem {

    String getExternalId();
    String getName();
    String getSupplierExternalId();
    String getSupplierName();
    String getSupplierProvince();
    BigDecimal getBoxWeight();
    BigDecimal getUnitPrice();
    String getUnitOfMeasure();
    String getCategory();
    String getNotes();
    String getFrequency();
    Optional<QuantityConstraints> getQuantityConstraints();
}
