package eu.aequos.gogas.excel;

import eu.aequos.gogas.excel.order.OrderExportRequest;
import eu.aequos.gogas.excel.products.ExcelPriceListItem;
import eu.aequos.gogas.excel.products.ExtractProductsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "excelservice", url = "${excel.service.url}", path = "/api/excel")
public interface ExcelServiceClient {

    @PostMapping(value = "order")
    byte[] order(@RequestBody OrderExportRequest orderExportRequest);

    @PostMapping(value = "products/generate")
    byte[] products(@RequestBody List<ExcelPriceListItem> products);

    @PostMapping(value = "products/extract/{excelType}")
    ExtractProductsResponse extractProducts(@RequestBody byte[] excelContent, @PathVariable("excelType") String excelType);
}
