package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.*;
import edu.icet.ecom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductVariantRepository variantRepository;
    private final StockLogRepository logRepository;
    private final ProductRepository productRepository;
    private final StockReportRepository reportRepository;
    private final SaleRepository saleRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    // --- Data Retrieval ---
    public List<StockReportEntity> getAllSavedReports() {
        return reportRepository.findAll();
    }

    public List<StockLogEntity> getAllStockLogs() {
        return logRepository.findAll(); // Fetches the data from STOCK_LOG table
    }

    // --- Logic ---
    @Transactional
    public void updateStock(StockUpdateDto dto) {
        try {
            ProductVariantEntity variant = variantRepository.findByBarcodeId(dto.getBarcodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barcode not found"));

            String now = LocalDateTime.now().format(FORMATTER);

            StockLogEntity log = new StockLogEntity();
            log.setVariant(variant);
            log.setBarcodeId(variant.getBarcodeId());
            log.setQuantityChange(dto.getQuantityAdded());
            log.setUpdateReason("RESTOCK");
            log.setTimestamp(now);
            logRepository.saveAndFlush(log);

            variant.setStockQuantity((variant.getStockQuantity() != null ? variant.getStockQuantity() : 0) + dto.getQuantityAdded());
            variantRepository.saveAndFlush(variant);

            // Notify UI
            StockReportDto updatedReport = generateReport("DAILY", LocalDate.now().toString());
            messagingTemplate.convertAndSend("/topic/stock-reports", updatedReport);
            refreshStatus(variant.getProduct().getProductId());

        } catch (DataIntegrityViolationException e) {
            System.out.println("Duplicate transaction blocked");
        }
    }

    @Transactional
    public StockReportDto generateReport(String type, String date) {
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(date);
        List<SaleEntity> sales = saleRepository.findByTimestampStartingWith(date);

        int totalIn = 0; int totalOut = 0; double soldValue = 0.0; double discounts = 0.0; double revenue = 0.0;

        for (SaleEntity sale : sales) {
            discounts += (sale.getDiscountAmount() != null) ? sale.getDiscountAmount() : 0.0;
            revenue += (sale.getNetAmount() != null) ? sale.getNetAmount() : 0.0;
        }

        for (StockLogEntity log : logs) {
            int qty = (log.getQuantityChange() != null) ? log.getQuantityChange() : 0;
            if (qty > 0) totalIn += qty;
            else if (qty < 0) {
                totalOut += Math.abs(qty);
                ProductVariantEntity v = log.getVariant();
                if (v != null) {
                    double p = (v.getPriceOverride() != null) ? v.getPriceOverride() : v.getProduct().getBasePrice();
                    soldValue += (Math.abs(qty) * p);
                }
            }
        }

        StockReportDto dto = new StockReportDto();
        dto.setReportType(type.toUpperCase());
        dto.setDate(date);
        dto.setTotalItemsIn(totalIn);
        dto.setTotalItemsOut(totalOut);
        dto.setTotalRevenue(revenue);
        dto.setTotalDiscountGiven(discounts);
        dto.setStockValue(calculateStockValue());

        saveOrUpdateReport(dto, soldValue);
        return dto;
    }

    private void saveOrUpdateReport(StockReportDto dto, double soldItemsValue) {
        List<StockReportEntity> existing = reportRepository.findByReportDateAndReportType(dto.getDate(), dto.getReportType());
        StockReportEntity entity = existing.isEmpty() ? new StockReportEntity() : existing.get(0);

        entity.setReportType(dto.getReportType());
        entity.setReportDate(dto.getDate());
        entity.setTotalItemsIn(dto.getTotalItemsIn());
        entity.setTotalItemsOut(dto.getTotalItemsOut());
        entity.setTotalRevenue(dto.getTotalRevenue());
        entity.setTotalDiscountGiven(dto.getTotalDiscountGiven());
        entity.setSoldItemsValue(soldItemsValue);
        entity.setStockValue(dto.getStockValue());
        entity.setGeneratedAt(LocalDateTime.now().format(FORMATTER));

        reportRepository.saveAndFlush(entity);
    }

    private double calculateStockValue() {
        return productRepository.findAll().stream()
                .flatMap(p -> p.getVariants().stream())
                .mapToDouble(v -> (v.getStockQuantity() != null ? v.getStockQuantity() : 0) * (v.getPriceOverride() != null ? v.getPriceOverride() : v.getProduct().getBasePrice()))
                .sum();
    }

    private void refreshStatus(Integer productId) {
        productRepository.findById(productId).ifPresent(p -> {
            int stock = p.getVariants().stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
            p.setStockStatus(stock <= 0 ? StockStatus.OUT_OF_STOCK : stock < 10 ? StockStatus.LOW_STOCK : StockStatus.AVAILABLE);
            productRepository.save(p);
        });
    }
}