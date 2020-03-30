package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "dateordini")
public class Order {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "iddateordini" , columnDefinition="uniqueidentifier")
    private String id;
    
    @Column(name = "stato", nullable = false)
    private int statusCode;
    
    @Column(name = "dataapertura", nullable = false)
    private LocalDate openingDate;

    @Column(name = "datachiusura", nullable = false)
    private LocalDate dueDate;

    @Column(name = "orachiusura", nullable = false)
    private int dueHour;

    @Column(name = "dataconsegna", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipologiaordine", nullable = false)
    private OrderType orderType;

    @Column(name = "idordineesterno")
    private String externalOrderId;
    
    @Column(name = "inviato", nullable = false)
    private boolean sent;
    
    @Column(name = "spesetrasporto", nullable = false)
    private BigDecimal shippingCost;
    
    @Column(name = "externallink")
    private String externaLlink;
    
    @Column(name = "invoiceamount")
    private java.math.BigDecimal invoiceAmount;
    
    @Column(name = "invoicenumber")
    private String invoiceNumber;
    
    @Column(name = "attachmenttype")
    private String attachmentType;
    
    @Column(name = "invoicedate")
    private LocalDate invoiceDate;
    
    @Column(name = "paid", nullable = false)
    private boolean paid;
    
    @Column(name = "paymentdate")
    private LocalDate paymentDate;
    
    @Column(name = "lastsynchro")
    private LocalDateTime lastSynchro;
    
    @Column(name = "lastweightupdate")
    private LocalDateTime lastWeightUpdate;

    public LocalDateTime getDueDateAndTime() {
        return this.dueDate.atTime(this.dueHour, 0);
    }
}
