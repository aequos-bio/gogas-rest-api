package eu.aequos.gogas.integration.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "aequos-api", url = "http://order.aequos.bio")
@RequestMapping(value = "order/services")
public interface AequosApiClient {

    @GetMapping(value = "tipi_ordine.php")
    List<AequosOrderType> orderTypes();

    @GetMapping(value = "ordini_aperti.php")
    List<AequosOpenOrder> openOrders();

    @GetMapping(value = "listino.php")
    AequosPriceList getPriceList(@RequestParam(name = "tipo_ordine") int orderTypeId);

    @PostMapping(value = "crea_ordine.php", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    OrderCreatedResponse createOrder(@RequestBody Map<String, ?> formParams);

//    @PostMapping(value = "ottieni_ordine.php", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    OrderSynchResponse synchOrder(@RequestBody Map<String, ?> formParams);

    @PostMapping(value = "ottieni_ordine.php", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    String synchOrder2(@RequestBody Map<String, ?> formParams);

    @PostMapping(value = "update_pesi.php", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    WeightsUpdatedResponse updateWeight(@RequestBody Map<String, ?> formParams);
}
