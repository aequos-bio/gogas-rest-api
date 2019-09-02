package eu.aequos.gogas.integration.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "aequos-api", url = "http://order.aequos.eu")
@RequestMapping(value = "order/services")
public interface AequosApiClient {

    @GetMapping(value = "tipi_ordine.php")
    List<AequosOrderType> orderTypes();

    @GetMapping(value = "ordini_aperti.php")
    List<AequosOpenOrder> openOrders();

    @GetMapping(value = "listino.php")
    AequosPriceList getPriceList(@RequestParam(name = "tipo_ordine") int orderTypeId);
}
