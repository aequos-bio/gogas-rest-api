package eu.aequos.gogas.dto;

import eu.aequos.gogas.persistence.entity.Menu;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class MenuDTO {
    private String id;
    private String label;
    private String url;
    private List<MenuDTO> subMenus;
    private boolean external;

    public MenuDTO(Menu menu) {
        this(menu, new ArrayList<>());
    }

    public MenuDTO(Menu menu, List<Menu> subMenus) {
        this.id = menu.getId();
        this.label = menu.getLabel();
        this.url = menu.getUrl();
        this.external = menu.isExternal();

        this.subMenus = subMenus.stream()
                .map(MenuDTO::new)
                .collect(Collectors.toList());
    }
}
