package eu.aequos.gogas.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class AboutDTO {
    private String name;
    private String description;
    private String version;
    private List<String> authors;
    private String copyrights;
}
