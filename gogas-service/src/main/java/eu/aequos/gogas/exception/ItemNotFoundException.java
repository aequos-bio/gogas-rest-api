package eu.aequos.gogas.exception;

public class ItemNotFoundException extends GoGasException {

    private static final String MESSAGE_TEMPLATE = "Item not found. Type: %s, Id: %s";

    private String itemType;
    private Object itemId;

    public ItemNotFoundException(String itemName, Object itemId) {
        super();
        this.itemType = itemName;
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public Object getItemId() {
        return itemId;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_TEMPLATE, itemType, itemId);
    }
}
