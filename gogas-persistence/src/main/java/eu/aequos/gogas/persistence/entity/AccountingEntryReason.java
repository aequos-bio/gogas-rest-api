package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.function.Function;

@Data
@Entity
@Table(name = "causale")
public class AccountingEntryReason {

    public enum Sign {
        PLUS("+", Function.identity()),
        MINUS("-", BigDecimal::negate);

        private String symbol;
        private Function<BigDecimal, BigDecimal> function;

        Sign(String symbol, Function<BigDecimal, BigDecimal> operation) {
            this.symbol = symbol;
            this.function = operation;
        }

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getSignedAmount(BigDecimal absoluteAmount) {
            if (absoluteAmount == null)
                return absoluteAmount;

            return function.apply(absoluteAmount);
        }

        public static Sign fromSymbol(String symbol) {
            if ("+".equals(symbol))
                return Sign.PLUS;

            if ("-".equals(symbol))
                return Sign.MINUS;

            throw new NoSuchElementException("Symbol not recognized: " + symbol);
        }
    }

    @Id
    @Column(name = "codicecausale", nullable = false)
    private String reasonCode;

    @Column(name = "segno", nullable = false)
    private String sign;

    @Column(name = "descrizione", nullable = false)
    private String description;

    @Column(name = "codicecontabile", nullable = true)
    private String accountingCode;

    public AccountingEntryReason withReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
        return this;
    }

    public Sign getSignEnum() {
        return Sign.fromSymbol(sign);
    }
}
