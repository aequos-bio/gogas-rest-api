package eu.aequos.gogas.attachments;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AttachmentRepo {
    private final String rootFolder;

    public AttachmentRepo(@Value("${attachments.rootfolder}") String rootFolder) {
        this.rootFolder = rootFolder;
    }
}
