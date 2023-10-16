package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "dateordini")
public class Order {

    public enum OrderStatus {
        Opened(0, "Aperto"),
        Closed(1, "Chiuso"),
        Accounted(2, "Contabilizzato"),
        Cancelled(3, "Annullato");

        private int statusCode;
        private String description;

        OrderStatus(int statusCode, String description) {
            this.statusCode = statusCode;
            this.description = description;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getDescription() {
            return description;
        }

        public boolean isOpen() {
            return this == Opened;
        }
    }

    private static Map<Integer, OrderStatus> orderStatusMap = Arrays.stream(OrderStatus.values())
            .collect(Collectors.toMap(OrderStatus::getStatusCode, Function.identity()));

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

    public OrderStatus getStatus() {
        return orderStatusMap.get(this.statusCode);
    }

    public LocalDateTime getDueDateAndTime() {
        return this.dueDate.atTime(this.dueHour, 0);
    }

    public boolean isEditable() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(openingDate.atStartOfDay()) && now.isBefore(this.getDueDateAndTime());
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(getDueDateAndTime());
    }

    public boolean isNotYetOpened() {
        return LocalDate.now().isBefore(openingDate);
    }

    private boolean isDateTimeWithinMinutesFromNow(LocalDateTime orderDate, int minutes, Clock clock) {
        return ChronoUnit.MINUTES.between(LocalDateTime.now(clock), orderDate) < minutes;
    }
}
