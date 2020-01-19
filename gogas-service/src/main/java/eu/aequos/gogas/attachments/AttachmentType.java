package eu.aequos.gogas.attachments;

public enum AttachmentType {
    Invoice("Invoice");

    private String folderName;

    AttachmentType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
