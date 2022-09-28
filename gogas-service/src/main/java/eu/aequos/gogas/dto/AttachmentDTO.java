package eu.aequos.gogas.dto;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@AllArgsConstructor
public class AttachmentDTO {
    private final byte[] content;
    private final String contentType;
    private final String name;

    public void writeToHttpResponse(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
        response.setContentType(contentType);
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }

    @SneakyThrows
    public void addToMail(MimeMessageHelper helper) {
        helper.addAttachment(name, new ByteArrayDataSource(content, contentType));
    }
}
