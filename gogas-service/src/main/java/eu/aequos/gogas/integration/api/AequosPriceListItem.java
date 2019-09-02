package eu.aequos.gogas.integration.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AequosPriceListItem {

    @JsonProperty("codice")
    String code;

    @JsonProperty("descrizione")
    String description;

    @JsonProperty("cod_forn")
    String supplierCode;

    @JsonProperty("rag")
    String supplierDescription;

    @JsonProperty("piva")
    String vatCode;

    @JsonProperty("provincia")
    String province;

    @JsonProperty("disponibile")
    String available;

    @JsonProperty("peso_collo")
    BigDecimal boxWight;

    @JsonProperty("prezzo")
    BigDecimal price;

    @JsonProperty("um")
    String unitOfMeasure;

    @JsonProperty("categoria")
    String category;

    @JsonProperty("note")
    String notes;

    @JsonProperty("cadenza")
    String frequency;

    @JsonProperty("solo_collo")
    boolean boxOnly;

    @JsonProperty("multiplo")
    BigDecimal multiple;
}
