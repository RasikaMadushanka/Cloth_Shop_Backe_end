package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.service.StockService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@CrossOrigin
public class Stock_Controller {
    private final StockService stockService;

    @PostMapping("/update")
    public ResponseEntity<String>updateStock(@RequestBody StockUpdateDto stockUpdateDto){
        stockService.updateStock(stockUpdateDto);
        return ResponseEntity.ok("Stock updated successfully");
    }
    @GetMapping("reports/daily")
    public ResponseEntity<StockReportDto>getDaily(@RequestParam String date){
        return ResponseEntity.ok(stockService.generateReport("Daily",date));
    }
    @GetMapping("reports/monthly")
    public ResponseEntity<StockReportDto>getMonthly(@RequestParam String date){
        return ResponseEntity.ok(stockService.generateReport("Monthly",date));
    }
    @GetMapping("reports/Yearly")
    public ResponseEntity<StockReportDto>getYearly(@RequestParam String date){
        return ResponseEntity.ok(stockService.generateReport("Yearly",date));
    }


}
