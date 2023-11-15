package eu.aequos.gogas.attachments;

public enum AttachmentType {
    INVOICE("invoice"),
    LOGO("logo");

    private String folderName;

    AttachmentType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
