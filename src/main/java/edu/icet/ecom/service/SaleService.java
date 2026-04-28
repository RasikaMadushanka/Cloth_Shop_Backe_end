package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.SalesDto;
import edu.icet.ecom.model.dto.SalesItemDto;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.entity.*;
import edu.icet.ecom.repository.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final StockLogRepository logRepository;
    private final AdminRepository adminRepository;
    private final StockService stockService;
    private final StockReportRepository stockReportRepository;

    @Transactional
    public void placeOrder(SalesDto salesDto) {
        SaleEntity saleEntity = new SaleEntity();
        saleEntity.setSaleId("SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        LocalDateTime now = LocalDateTime.now();
        // Use a consistent formatter to avoid parsing errors later
        String formattedTimestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        saleEntity.setTimestamp(formattedTimestamp);
        saleEntity.setPaymentMethod(salesDto.getPaymentMethod());

        AdminEntity admin = adminRepository.findById(salesDto.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        saleEntity.setAdmin(admin);

        double totalAmount = 0.0;
        double totalDiscountAmount = 0.0;
        List<SalesItemEntity> itemEntities = new ArrayList<>();

        for (SalesItemDto itemDto : salesDto.getItems()) {
            ProductVariantEntity variant = variantRepository
                    .findByBarcodeId(itemDto.getBarcodeId())
                    .orElseThrow(() -> new RuntimeException("Barcode not found: " + itemDto.getBarcodeId()));

            if (variant.getStockQuantity() < itemDto.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + variant.getSku());
            }

            // Update Stock
            variant.setStockQuantity(variant.getStockQuantity() - itemDto.getQuantity());
            variantRepository.save(variant);

            // Pricing Logic
            double unitPrice = itemDto.getUnitPrice();
            int qty = itemDto.getQuantity();
            double itemTotal = unitPrice * qty;
            double pct = (salesDto.getDiscountPercentage() != null) ? salesDto.getDiscountPercentage() : 0.0;
            double itemDiscount = itemTotal * (pct / 100);
            double itemNet = itemTotal - itemDiscount;

            SalesItemEntity itemEntity = new SalesItemEntity();
            itemEntity.setSale(saleEntity);
            itemEntity.setVariant(variant);
            itemEntity.setQuantity(qty);
            itemEntity.setUnitPrice(unitPrice);
            itemEntity.setTotalPrice(itemTotal);
            itemEntity.setDiscountAmount(itemDiscount);
            itemEntity.setNetPrice(itemNet);
            itemEntities.add(itemEntity);

            totalAmount += itemTotal;
            totalDiscountAmount += itemDiscount;

            // ✅ FIX: Set timestamp as String, not LocalDateTime
            StockLogEntity log = new StockLogEntity();
            log.setVariant(variant);
            log.setBarcodeId(variant.getBarcodeId());
            log.setQuantityChange(-qty);
            log.setUpdateReason("SALE: " + saleEntity.getSaleId());
            log.setTimestamp(formattedTimestamp);
            log.setAdmin(admin);

            logRepository.save(log);

            stockService.refreshStatus(variant.getProduct().getProductId());
        }

        saleEntity.setTotalAmount(totalAmount);
        saleEntity.setDiscountPercentage(salesDto.getDiscountPercentage() != null ? salesDto.getDiscountPercentage() : 0.0);
        saleEntity.setDiscountAmount(totalDiscountAmount);
        saleEntity.setNetAmount(totalAmount - totalDiscountAmount);
        saleEntity.setItems(itemEntities);

        saleRepository.save(saleEntity);
    }




    public @Nullable StockReportDto generateReport(String type, String date) {
        // 1. Fetch Sales and Stock Logs for the given date pattern
        // (Ensure your repositories have findByTimestampStartingWith and findByTimestampPattern)
        List<SaleEntity> sales = saleRepository.findByTimestampStartingWith(date);
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(date);

        if (sales.isEmpty() && logs.isEmpty()) {
            return null; // Returning null if no data exists for that day
        }

        int itemsOut = 0;
        int itemsIn = 0;
        double revenue = 0.0;
        double totalDiscount = 0.0;

        // 2. Calculate Sales Totals (Items Out, Revenue, Discounts)
        for (SaleEntity sale : sales) {
            revenue += (sale.getNetAmount() != null) ? sale.getNetAmount() : 0.0;
            totalDiscount += (sale.getDiscountAmount() != null) ? sale.getDiscountAmount() : 0.0;

            if (sale.getItems() != null) {
                itemsOut += sale.getItems().stream()
                        .mapToInt(SalesItemEntity::getQuantity)
                        .sum();
            }
        }

        // 3. Calculate Stock Log Totals (Items In - Restocks)
        for (StockLogEntity log : logs) {
            if (log.getQuantityChange() > 0) {
                itemsIn += log.getQuantityChange();
            }
        }

        // 4. Calculate Current Total Warehouse Value
        double currentStockValue = variantRepository.findAll().stream()
                .mapToDouble(v -> (v.getStockQuantity() != null ? v.getStockQuantity() : 0) * (v.getPriceOverride() != null ? v.getPriceOverride() : 0.0))
                .sum();

        // 5. Create and Save the Entity (For SQL Persistence)
        StockReportEntity reportEntity = new StockReportEntity();
        reportEntity.setReportType(type.toUpperCase());
        reportEntity.setReportDate(date);
        reportEntity.setTotalItemsIn(itemsIn);
        reportEntity.setTotalItemsOut(itemsOut);
        reportEntity.setTotalRevenue(revenue);
        reportEntity.setTotalDiscountGiven(totalDiscount);
        reportEntity.setStockValue(currentStockValue);
        reportEntity.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        stockReportRepository.save(reportEntity);

        // 6. Map and Return the DTO
        StockReportDto dto = new StockReportDto();
        dto.setReportType(reportEntity.getReportType());
        dto.setDate(reportEntity.getReportDate());
        dto.setTotalItemsIn(itemsIn);
        dto.setTotalItemsOut(itemsOut);
        dto.setTotalRevenue(revenue);
        dto.setTotalDiscountGiven(totalDiscount);
        dto.setStockValue(currentStockValue);

        return dto;
    }
}