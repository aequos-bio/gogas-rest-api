package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "notificationprefs")
public class NotificationPreferences {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "idutente", nullable = false)
    private String userId;

    @Column(name = "idtipologiaordine")
    private String orderTypeId;

    @Column(name = "apertura")
    private boolean onOrderOpened;

    @Column(name = "scadenza")
    private boolean onOrderExpiration;

    @Column(name = "minutiscadenza")
    private int onExpirationMinutesBefore;

    @Column(name = "consegna")
    private boolean onOrderDelivery;

    @Column(name = "minuticonsegna")
    private int onDeliveryMinutesBefore;

    @Column(name = "aggiornamentoqta")
    private boolean onOrderUpdatedQuantity;

    @Column(name = "contabilizzazione")
    private boolean onOrderAccounted;
}