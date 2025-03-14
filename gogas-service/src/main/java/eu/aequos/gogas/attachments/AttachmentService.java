package eu.aequos.gogas.attachments;

import eu.aequos.gogas.dto.AttachmentDTO;
import eu.aequos.gogas.exception.GoGasException;
import eu.aequos.gogas.exception.ItemNotFoundException;
import eu.aequos.gogas.multitenancy.TenantContext;
import eu.aequos.gogas.persistence.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@Service
public class AttachmentService {

    private final AttachmentRepo attachmentRepo;

    public void storeAttachment(byte[] attachmentContent, AttachmentType type, String fileName) throws GoGasException {
        try {
            Path attachmentFilePath = resolveFilePath(type, fileName);
            Files.write(attachmentFilePath, attachmentContent);
        } catch (IOException ex) {
            log.error("Error while saving attachment {} of type {}", fileName, type, ex);
            throw new GoGasException("Unable to save attachment");
        }
    }

    public boolean hasAttachment(AttachmentType type, String fileName) throws GoGasException {
        try {
            Path attachmentFilePath = resolveFilePath(type, fileName);
            return Files.exists(attachmentFilePath);
        } catch (IOException ex) {
            log.error("Error while checking attachment {} of type {}", fileName, type, ex);
            return false;
        }
    }

    public byte[] retrieveAttachment(AttachmentType type, String fileName) throws GoGasException {
        try {
            Path attachmentFilePath = resolveFilePath(type, fileName);

            if (Files.notExists(attachmentFilePath))
                throw new ItemNotFoundException("attachment", fileName);

            return Files.readAllBytes(attachmentFilePath);
        } catch (IOException ex) {
            log.error("Error while retrieving attachment {} of type {}", fileName, type, ex);
            throw new GoGasException("Unable to read attachment");
        }
    }

    public void removeAttachment(AttachmentType type, String fileName) {
        try {
            Path attachmentFilePath = resolveFilePath(type, fileName);

            if (Files.notExists(attachmentFilePath))
                throw new ItemNotFoundException("attachment", fileName);

            Files.delete(attachmentFilePath);
        } catch (IOException ex) {
            log.error("Error while removing attachment {} of type {}", fileName, type, ex);
            throw new GoGasException("Unable to remove attachment");
        }
    }

    public AttachmentDTO buildAttachmentDTO(Order order, byte[] attachmentContent, String contentType) {
        String fileName = buildFileNameFromOrder(order.getOrderType().getDescription(),
                order.getDeliveryDate(), "", contentType);

        return new AttachmentDTO(attachmentContent, contentType, fileName);
    }

    public AttachmentDTO buildAttachmentDTO(Order order, byte[] attachmentContent, String suffix, String contentType) {
        String fileName = buildFileNameFromOrder(order.getOrderType().getDescription(),
                order.getDeliveryDate(), suffix, contentType);

        return new AttachmentDTO(attachmentContent, contentType, fileName);
    }

    public AttachmentDTO buildAttachmentDTO(String filename, byte[] attachmentContent, String contentType) {
        String fileName = buildFileName(filename, contentType);
        return new AttachmentDTO(attachmentContent, contentType, fileName);
    }

    public String buildFileNameFromOrder(String description, LocalDate date, String suffix, String mimeType) {
        String orderType = description.replace(" ", "_");
        String deliveryDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String extensionWithDot = resolveFileExtension(mimeType);

        return String.format("%s-%s%s%s", orderType, deliveryDate, suffix, extensionWithDot);
    }

    public String buildFileName(String name, String mimeType) {
        String extensionWithDot = resolveFileExtension(mimeType);
        return String.format("%s%s", name, extensionWithDot);
    }

    private String resolveFileExtension(String mimeType) {
        try {
            return MimeTypes.getDefaultMimeTypes()
                    .forName(mimeType)
                    .getExtension();

        } catch (MimeTypeException e) {
            log.error("Unable to determine extension for mime type {}", mimeType);
            return "";
        }
    }

    private Path resolveFilePath(AttachmentType type, String fileName) throws IOException, GoGasException {
        String tenantId = TenantContext.getTenantId()
                .orElseThrow(() -> new GoGasException("Invalid tenant"));

        Path folderPath = Paths.get(attachmentRepo.getRootFolder(), tenantId, type.getFolderName());

        if (Files.notExists(folderPath))
            Files.createDirectories(folderPath);

        return folderPath.resolve(fileName.toLowerCase());
    }
}
