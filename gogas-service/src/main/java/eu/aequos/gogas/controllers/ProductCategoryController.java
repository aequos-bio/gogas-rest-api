package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.security.annotations.IsAdmin;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("category")
public class ProductCategoryController {

    private ProductCategoryRepo productCategoryRepo;

    public ProductCategoryController(ProductCategoryRepo productCategoryRepo) {
        this.productCategoryRepo = productCategoryRepo;
    }

    @GetMapping(value = "list/{productType}")
    public List<SelectItemDTO> list(@PathVariable String productType) {
         return productCategoryRepo.findByOrderTypeId(productType).stream()
                 .map(c -> new SelectItemDTO(c.getId(), c.getDescription()))
                 .collect(Collectors.toList());
    }

    @IsAdmin
    @PostMapping(value = "{orderTypeId}")
    public String create(@PathVariable String orderTypeId, @RequestBody String category) {
        ProductCategory productCategory = new ProductCategory();
        productCategory.setDescription(category);
        productCategory.setOrderTypeId(orderTypeId);

        return productCategoryRepo.save(productCategory).getId();
    }

    @IsAdmin
    @DeleteMapping(value = "{categoryId}")
    public BasicResponseDTO delete(@PathVariable String categoryId) {
        productCategoryRepo.deleteById(categoryId);
        return new BasicResponseDTO("OK");
    }
}
