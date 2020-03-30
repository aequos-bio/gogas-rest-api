package eu.aequos.gogas.order;

public enum OrderStatus {
    Opened(0, "Aperto"),
    Closed(1, "Chiuso"),
    Accounted(2, "Contabilizzato"),
    Cancelled(3, "Annullato");

    private int statusCode;
    private String description;

    OrderStatus(int statusCode, String description) {
        this.statusCode = statusCode;
        this.description = description;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOpen() {
        return this == Opened;
    }
}
