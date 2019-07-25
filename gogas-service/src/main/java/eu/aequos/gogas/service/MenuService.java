package eu.aequos.gogas.service;

import eu.aequos.gogas.persistence.entity.Menu;
import eu.aequos.gogas.dto.MenuDTO;
import eu.aequos.gogas.persistence.entity.MenuByRole;
import eu.aequos.gogas.persistence.repository.MenuRepo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private MenuRepo menuRepo;

    public MenuService(MenuRepo menuRepo) {
        this.menuRepo = menuRepo;
    }

    public List<MenuDTO> getMenuTreeByRole(String role) {
        List<MenuByRole> flatMenuList = menuRepo.findByIdRole(role);

        return flatMenuList.stream().filter(m -> m.getId().getMenu().getParentMenuId() == null)
                .sorted(Comparator.comparingInt(MenuByRole::getOrder))
                .map(m -> new MenuDTO(m.getMenu(), getSubMenus(m, flatMenuList)))
                .collect(Collectors.toList());
    }

    private List<Menu> getSubMenus(MenuByRole parentMenu, List<MenuByRole> flatMenuList) {
        return flatMenuList.stream()
                .filter(s -> parentMenu.getMenu().getId().equals(s.getMenu().getParentMenuId()))
                .sorted(Comparator.comparingInt(MenuByRole::getOrder))
                .map(MenuByRole::getMenu)
                .collect(Collectors.toList());
    }
}
