package eu.aequos.gogas.persistence.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderSummaryId implements Serializable {

    private String orderId;
    private String userId;
}
