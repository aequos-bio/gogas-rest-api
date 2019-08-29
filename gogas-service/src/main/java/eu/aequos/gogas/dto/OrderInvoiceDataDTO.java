package eu.aequos.gogas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
public class OrderInvoiceDataDTO {

    @JsonProperty("idDataOrdine")
    String orderId;

    @JsonProperty("numeroFattura")
    String invoiceNumber;

    @JsonProperty("importoFattura")
    BigDecimal invoiceAmount;

    @JsonProperty("dataFattura")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    Date invoiceDate;

    @JsonProperty("dataPagamento")
    @JsonFormat(shape = STRING, pattern = "dd/MM/yyyy")
    Date paymentDate;

    @JsonProperty("pagato")
    boolean paid;
}
