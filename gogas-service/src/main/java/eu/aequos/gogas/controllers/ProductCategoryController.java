package eu.aequos.gogas.controllers;

import eu.aequos.gogas.dto.SelectItemDTO;
import eu.aequos.gogas.persistence.repository.ProductCategoryRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<SelectItemDTO> listProductCategories(@PathVariable String productType) {
         return productCategoryRepo.findByOrderTypeId(productType).stream()
                 .map(c -> new SelectItemDTO(c.getId(), c.getDescription()))
                 .collect(Collectors.toList());
    }
}
