package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.BasicResponseDTO;
import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.entity.ProductCategory;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import eu.aequos.gogas.security.annotations.IsAdmin;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Api("Product categories")
@RestController
@RequestMapping("api/category")
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
    public BasicResponseDTO create(@PathVariable String orderTypeId, @RequestBody String category) {
        ProductCategory productCategory = new ProductCategory();
        productCategory.setDescription(category);
        productCategory.setOrderTypeId(orderTypeId);

        String productCategoryId = productCategoryRepo.save(productCategory).getId();
        return new BasicResponseDTO(productCategoryId);
    }

    @IsAdmin
    @PutMapping(value = "{orderTypeId}")
    public BasicResponseDTO update(@PathVariable String orderTypeId, @RequestBody SelectItemDTO category) {
        ProductCategory productCategory = new ProductCategory();
        productCategory.setId(category.getId());
        productCategory.setDescription(category.getDescription());
        productCategory.setOrderTypeId(orderTypeId);

        String productCategoryId = productCategoryRepo.save(productCategory).getId();
        return new BasicResponseDTO(productCategoryId);
    }

    @IsAdmin
    @DeleteMapping(value = "{categoryId}")
    public BasicResponseDTO delete(@PathVariable String categoryId) {
        productCategoryRepo.deleteById(categoryId);
        return new BasicResponseDTO("OK");
    }
}
