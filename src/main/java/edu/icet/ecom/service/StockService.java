package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.SaleEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.model.entity.StockReportEntity;
import edu.icet.ecom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional
    public void updateStock(StockUpdateDto dto) {
        try {
            // 1. Fetch Variant
            ProductVariantEntity variant = variantRepository.findByBarcodeId(dto.getBarcodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + dto.getBarcodeId()));

            // 2. Generate Timestamp once for both Variant update and Log
            String now = LocalDateTime.now().format(FORMATTER);

            // 3. Create Log (Hibernate will check the Unique Constraint here)
            StockLogEntity log = new StockLogEntity();
            log.setVariant(variant);
            log.setBarcodeId(variant.getBarcodeId());
            log.setQuantityChange(dto.getQuantityAdded());
            log.setUpdateReason("RESTOCK");
            log.setTimestamp(now);
            logRepository.saveAndFlush(log); // Force the check immediately

            // 4. Update Variant Stock only if Log was successful
            int addedQty = dto.getQuantityAdded();
            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            variant.setStockQuantity(currentStock + addedQty);
            variantRepository.saveAndFlush(variant);

            // 5. Trigger Report Sync & WebSocket
            StockReportDto updatedReport = generateReport("DAILY", LocalDate.now().toString());
            messagingTemplate.convertAndSend("/topic/stock-reports", updatedReport);

            refreshStatus(variant.getProduct().getProductId());

        } catch (DataIntegrityViolationException e) {
            // This catches the double-click/duplicate 35!
            System.out.println("Duplicate transaction blocked: " + e.getMessage());
        }
    }

    @Transactional
    public StockReportDto generateReport(String type, String date) {
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(date);

        // 1. Get Sales for the same date to calculate real Revenue and Discounts
        List<SaleEntity> sales = saleRepository.findByTimestampStartingWith(date);

        int totalIn = 0;
        int totalOut = 0;
        double soldItemsValueToday = 0.0;
        double totalDiscounts = 0.0;
        double actualRevenue = 0.0;

        // Calculate Discounts and Net Revenue from Sale Entities
        for (SaleEntity sale : sales) {
            totalDiscounts += (sale.getDiscountAmount() != null) ? sale.getDiscountAmount() : 0.0;
            actualRevenue += (sale.getNetAmount() != null) ? sale.getNetAmount() : 0.0;
        }

        // Calculate Stock movement and Sold Items Value from Logs
        for (StockLogEntity log : logs) {
            int qty = (log.getQuantityChange() != null) ? log.getQuantityChange() : 0;
            if (qty > 0) {
                totalIn += qty;
            } else if (qty < 0) {
                int soldCount = Math.abs(qty);
                totalOut += soldCount;
                ProductVariantEntity variant = log.getVariant();
                if (variant != null) {
                    double price = (variant.getPriceOverride() != null)
                            ? variant.getPriceOverride() : variant.getProduct().getBasePrice();
                    soldItemsValueToday += (soldCount * price);
                }
            }
        }

        StockReportDto dto = new StockReportDto();
        dto.setReportType(type.toUpperCase());
        dto.setDate(date);
        dto.setTotalItemsIn(totalIn);
        dto.setTotalItemsOut(totalOut);
        dto.setTotalRevenue(actualRevenue); // Real money after discounts
        dto.setTotalDiscountGiven(totalDiscounts); // Fixing the NULL issue
        dto.setStockValue(calculateStockValue()); // Current warehouse value

        saveOrUpdateReport(dto, soldItemsValueToday);
        return dto;
    }

    private void saveOrUpdateReport(StockReportDto dto, double soldItemsValue) {
        List<StockReportEntity> existingReports = reportRepository
                .findByReportDateAndReportType(dto.getDate(), dto.getReportType());

        StockReportEntity entity;

        if (!existingReports.isEmpty()) {
            entity = existingReports.get(0);
            // DO NOT update entity.setStockValue(dto.getStockValue()) here.
            // This keeps the "Initial Stock Value" frozen from the first generation.

            if (existingReports.size() > 1) {
                for (int i = 1; i < existingReports.size(); i++) {
                    reportRepository.delete(existingReports.get(i));
                }
            }
        } else {
            entity = new StockReportEntity();
            // ONLY set the stock value when the report is created for the first time
            entity.setStockValue(dto.getStockValue());
        }

        entity.setReportType(dto.getReportType());
        entity.setReportDate(dto.getDate());
        entity.setTotalItemsIn(dto.getTotalItemsIn());
        entity.setTotalItemsOut(dto.getTotalItemsOut());
        entity.setTotalRevenue(dto.getTotalRevenue());
        entity.setTotalDiscountGiven(dto.getTotalDiscountGiven()); // Saving the discount
        entity.setSoldItemsValue(soldItemsValue);
        entity.setGeneratedAt(LocalDateTime.now().format(FORMATTER));

        reportRepository.saveAndFlush(entity);
    }

    private double calculateStockValue() {
        return productRepository.findAll().stream()
                .flatMap(product -> product.getVariants().stream())
                .mapToDouble(variant -> {
                    int qty = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                    double price = (variant.getPriceOverride() != null)
                            ? variant.getPriceOverride() : variant.getProduct().getBasePrice();
                    return qty * price;
                }).sum();
    }

    public void refreshStatus(Integer productId) {
        productRepository.findById(productId).ifPresent(product -> {
            int totalStock = product.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() == null ? 0 : v.getStockQuantity()).sum();
            product.setStockStatus(totalStock <= 0 ? StockStatus.OUT_OF_STOCK :
                    totalStock < 10 ? StockStatus.LOW_STOCK : StockStatus.AVAILABLE);
            productRepository.save(product);
        });
    }
}