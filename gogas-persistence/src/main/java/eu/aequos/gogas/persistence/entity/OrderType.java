package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "tipologiaordine")
public class OrderType {

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "idtipologiaordine" , columnDefinition="uniqueidentifier")
    private String id;

    @Column(name = "tipoordine", nullable = false)
    private String description;

    @Column(name = "riepilogo", nullable = false)
    private boolean summaryRequired;

    @Column(name = "totalecalcolato", nullable = false)
    private boolean computedAmount;

    @Column(name = "mostrapreventivo", nullable = false)
    private boolean showAdvance;

    @Column(name = "\"mostracompletamentocolli\"", nullable = false)
    private boolean showBoxCompletion;

    @Column(name = "prevedeturni", nullable = false)
    private boolean hasTurns;

    @Column(name = "excelallusers", nullable = false)
    private boolean excelAllUsers;

    @Column(name = "excelallproducts", nullable = false)
    private boolean excelAllProducts;

    @Column(name = "external", nullable = false)
    private boolean external;
    
    @Column(name = "externallink")
    private String externallink;
    
    @Column(name = "lastsynchro")
    private Date lastsynchro;

    @Column(name = "idordineaequos")
    private Integer aequosOrderId;

    @OneToMany(mappedBy = "orderType")
    private List<OrderManager> managers;
}
