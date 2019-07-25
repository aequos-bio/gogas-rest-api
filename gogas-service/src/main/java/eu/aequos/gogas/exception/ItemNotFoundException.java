package eu.aequos.gogas.exception;

public class ItemNotFoundException extends GoGasException {

    private String itemName;

    public ItemNotFoundException(String itemName) {
        super();
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }
}
