package eu.aequos.gogas.persistence.entity;

import lombok.Data;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Setter
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

    public boolean onOrderOpened() {
        return onOrderOpened;
    }

    public boolean onOrderExpiration() {
        return onOrderExpiration;
    }

    public boolean onOrderDelivery() {
        return onOrderDelivery;
    }

    public boolean onOrderUpdatedQuantity() {
        return onOrderUpdatedQuantity;
    }

    public boolean onOrderAccounted() {
        return onOrderAccounted;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderTypeId() {
        return orderTypeId;
    }

    public int getOnExpirationMinutesBefore() {
        return onExpirationMinutesBefore;
    }

    public int getOnDeliveryMinutesBefore() {
        return onDeliveryMinutesBefore;
    }

    @Data
    public static class NotificationPreferencesViewPK implements Serializable {
        private String userId;
        private String orderTypeId;
    }
}

