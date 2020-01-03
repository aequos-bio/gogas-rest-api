package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@IdClass(NotificationPreferencesView.NotificationPreferencesViewPK.class)
@Table(name = "notificationprefsview")
public class NotificationPreferencesView {

    @Id
    @Column(name = "idutente", nullable = false)
    private String userId;

    @Id
    @Column(name = "idtipologiaordine")
    private String orderTypeId;

    @Column(name = "apertura")
    private boolean onOrderOpened;

    @Column(name = "apertura")
    private boolean onOrderExpiration;

    @Column(name = "minutiscadenza")
    private int onExpirationMinutesBefore;

    @Column(name = "consegna")
    private boolean onOrderDelivery;

    @Column(name = "minuticonsegna")
    private int onDeliveryMinutesBefore;

    @Column(name = "aggiornamentoqta")
    private boolean onOrderUpdatedQuantity;

    @Column(name = "aggiornamentoqta")
    private boolean onOrderAccounted;

    @Data
    public static class NotificationPreferencesViewPK implements Serializable {
        private final String userId;
        private final String orderTypeId;
    }
}

