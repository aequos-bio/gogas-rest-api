package eu.aequos.gogas.persistence.entity.derived;

public interface OpenOrderSummary extends OrderSummary {

    String getUserId();
    int getItemsCount();
}
