package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.SalesDto;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
@CrossOrigin
public class SaleController {
    private final SaleService saleService;
    @PostMapping("/place-order")
    public ResponseEntity<String>placeOrder(@RequestBody SalesDto salesDto){
        saleService.placeOrder(salesDto);
        // Implement order placement logic here
        return ResponseEntity.ok("Order placed successfully");
    }
    @GetMapping("/report/{type}/{date}")
    public ResponseEntity<StockReportDto>getReport(@PathVariable String type,@PathVariable String date){
        return ResponseEntity.ok(saleService.generateReport(type,date));
    }

}
