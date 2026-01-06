package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "schedacontabile")
@IdClass(UserBalanceEntry.Key.class)
public class UserBalanceEntry {

    @Data
    public static class Key implements Serializable {
        private String id;
        private BigDecimal amount;
    }

    @Id
    @Column(name = "idriga" , columnDefinition="uniqueidentifier", nullable = false)
    private String id;

    @Column(name = "idutente", columnDefinition="uniqueidentifier", nullable = false)
    private String userId;

    @Column(name = "data", nullable = false)
    private LocalDate date;

    @Column(name = "descrizione", nullable = false, length = 100)
    private String description;

    @Column(name = "segno", nullable = false)
    private String sign;

    @Id
    @Column(name = "importo", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "iddateordini", columnDefinition="uniqueidentifier")
    private String orderId;
}
