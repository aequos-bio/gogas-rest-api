package eu.aequos.gogas.persistence.entity.derived;

public interface UserOrderSummary extends OrderSummary {

    int getItemsCount();
    int getFriendCount();
    int getfriendAccounted();
}
