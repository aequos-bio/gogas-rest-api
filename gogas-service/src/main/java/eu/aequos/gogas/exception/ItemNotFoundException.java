package eu.aequos.gogas.exception;

import org.springframework.security.access.AccessDeniedException;

public class ItemNotFoundException extends AccessDeniedException {

    private static final String MESSAGE_TEMPLATE = "Item not found. Type: %s, Id: %s";

    private String itemType;
    private Object itemId;

    public ItemNotFoundException(String itemName, Object itemId) {
        super(String.format(MESSAGE_TEMPLATE, itemName, itemId));
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
