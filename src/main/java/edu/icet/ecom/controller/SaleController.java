package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.SalesDto;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
@CrossOrigin
public class SaleController {
    private final SaleService saleService;
    @GetMapping("/all")
    public ResponseEntity<List<SalesDto>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }
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
    // Add this inside SaleController.java
    @GetMapping("/find-by-barcode")
    public ResponseEntity<List<SalesDto>> findSalesByBarcode(@RequestParam String barcodeId) {
        return ResponseEntity.ok(saleService.findSalesByBarcode(barcodeId));
    }
    @PostMapping("/process-return")
    public ResponseEntity<Map<String, String>> processReturn(@RequestBody Map<String, Object> returnRequest) {
        // Expected JSON: { "saleId": "SALE-123", "barcodeId": "479...", "quantity": 1 }
        saleService.processReturn(returnRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Item successfully restocked and sale updated",
                "status", "SUCCESS"
        ));
    }

}
