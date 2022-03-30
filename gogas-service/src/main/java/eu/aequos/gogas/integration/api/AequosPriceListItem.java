package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.aequos.gogas.service.pricelist.ExternalPriceListItem;
import eu.aequos.gogas.service.pricelist.QuantityConstraints;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
public class AequosPriceListItem implements ExternalPriceListItem {

    @JsonProperty("codice")
    String externalId;

    @JsonProperty("descrizione")
    String name;

    @JsonProperty("cod_forn")
    String supplierExternalId;

    @JsonProperty("rag")
    String supplierName;

    @JsonProperty("piva")
    String vatCode;

    @JsonProperty("provincia")
    String supplierProvince;

    @JsonProperty("disponibile")
    String available;

    @JsonProperty("peso_collo")
    BigDecimal boxWeight;

    @JsonProperty("prezzo")
    BigDecimal unitPrice;

    @JsonProperty("um")
    String unitOfMeasure;

    @JsonProperty("categoria")
    String category;

    @JsonProperty("note")
    String notes;

    @JsonProperty("cadenza")
    String frequency;

    @Override
    public Optional<QuantityConstraints> getQuantityConstraints() {
        return Optional.empty();
    }
}
