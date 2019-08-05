package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
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
    private Date openingDate;

    @Column(name = "datachiusura", nullable = false)
    private Date dueDate;

    @Column(name = "orachiusura", nullable = false)
    private int dueHour;

    @Column(name = "dataconsegna", nullable = false)
    private Date deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipologiaordine", nullable = false)
    private OrderType orderType;

    @Column(name = "idordineesterno")
    private String externalOrderId;
    
    @Column(name = "inviato", nullable = false)
    private boolean sent;
    
    @Column(name = "spesetrasporto", nullable = false)
    private java.math.BigDecimal shippingCost;
    
    @Column(name = "externallink")
    private String externaLlink;
    
    @Column(name = "invoiceamount")
    private java.math.BigDecimal invoiceAmount;
    
    @Column(name = "invoicenumber")
    private String invoiceNumber;
    
    @Column(name = "attachmenttype")
    private String attachmentType;
    
    @Column(name = "invoicedate")
    private Timestamp invoiceDate;
    
    @Column(name = "paid", nullable = false)
    private boolean paid;
    
    @Column(name = "paymentdate")
    private Timestamp paymentDate;
    
    @Column(name = "lastsynchro")
    private Timestamp lastSynchro;
    
    @Column(name = "lastweightupdate")
    private Timestamp lastWeightUpdate;

    public OrderStatus getStatus() {
        return orderStatusMap.get(this.statusCode);
    }

    public Date getDueDateAndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.dueDate);
        calendar.set(Calendar.HOUR_OF_DAY, this.dueHour);
        return calendar.getTime();
    }

    public boolean isEditable() {
        Date now = new Date();
        return now.after(openingDate) && now.before(this.getDueDateAndTime());
    }
}
