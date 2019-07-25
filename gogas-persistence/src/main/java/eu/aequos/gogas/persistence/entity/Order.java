package eu.aequos.gogas.persistence.entity;

import eu.aequos.gogas.exception.UnknownOrderStatusException;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

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

        public static OrderStatus getByCode(int statusCode) throws UnknownOrderStatusException {
            return Arrays.stream(OrderStatus.values())
                    .filter(s -> s.getStatusCode() == statusCode)
                    .findAny()
                    .orElseThrow(() -> new UnknownOrderStatusException());
        }
    }

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

    @Column(name = "dataconsegna", nullable = false)
    private Date deliveryDate;

    @Column(name = "orachiusura", nullable = false)
    private int deliveryHour;

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

    @OneToMany
    @JoinColumn(name = "iddateordini")
    private Set<OrderSummary> orderSummaries;
    
    public OrderStatus getStatus() throws UnknownOrderStatusException {
        return OrderStatus.getByCode(this.statusCode);
    }

    public Date getDeliveryDateAndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.deliveryDate);
        calendar.set(Calendar.HOUR_OF_DAY, this.deliveryHour);
        return calendar.getTime();
    }

    public boolean isEditable() {
        return new Date().before(this.getDeliveryDateAndTime());
    }
}
