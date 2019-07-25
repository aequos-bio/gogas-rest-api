package eu.aequos.gogas.excel;

import eu.aequos.gogas.excel.order.OrderExportRequest;
import eu.aequos.gogas.excel.products.ProductPriceListExport;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "excelgenerator", url = "http://localhost:5000")
@RequestMapping(value = "api/excel")
public interface ExcelGeneratorClient {

    @PostMapping(value = "order")
    byte[] order(@RequestBody OrderExportRequest orderExportRequest);

    @PostMapping(value = "products")
    byte[] products(@RequestBody List<ProductPriceListExport> products);
}
