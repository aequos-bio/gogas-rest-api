package eu.aequos.gogas.dto;

import lombok.AllArgsConstructor;

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
}
