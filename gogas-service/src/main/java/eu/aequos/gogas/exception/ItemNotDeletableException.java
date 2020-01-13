package eu.aequos.gogas.exception;

public class ItemNotDeletableException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Impossibile eliminare l'elemento di tipo %s perchè ha dati associati";

    private String itemType;
    private Object itemId;

    public ItemNotDeletableException(String itemName, Object itemId) {
        super(String.format(MESSAGE_TEMPLATE, itemName));
        this.itemType = itemName;
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public Object getItemId() {
        return itemId;
    }
}
