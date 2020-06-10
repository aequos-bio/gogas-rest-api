package eu.aequos.gogas.exception;

public class DuplicatedItemException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Impossibile creare l'elemento di tipo %s perchè già esistente";

    private String itemType;
    private Object itemId;

    public DuplicatedItemException(String itemName, Object itemId) {
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
