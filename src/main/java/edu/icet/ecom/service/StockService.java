package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.model.entity.StockReportEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.StockLogRepository;
import edu.icet.ecom.repository.StockReportRepository;
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
        // The query 'findByTimestampPattern' uses LIKE 'YYYY-MM-DD%'
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(date);

        int totalIn = 0;
        int totalOut = 0;
        double totalRevenue = 0.0;

        for (StockLogEntity log : logs) {
            int qty = (log.getQuantityChange() != null) ? log.getQuantityChange() : 0;

            if (qty > 0) {
                // Counts INITIAL_STOCK_ADD, NEW_VARIANT_ADDED, and RESTOCK
                totalIn += qty;
            } else if (qty < 0) {
                // Counts Sales/Reductions
                int soldCount = Math.abs(qty);
                totalOut += soldCount;

                ProductVariantEntity variant = log.getVariant();
                if (variant != null) {
                    double price = (variant.getPriceOverride() != null)
                            ? variant.getPriceOverride()
                            : variant.getProduct().getBasePrice();
                    totalRevenue += (soldCount * price);
                }
            }
        }

        StockReportDto dto = new StockReportDto();
        dto.setReportType(type.toUpperCase());
        dto.setDate(date);
        dto.setTotalItemsIn(totalIn);
        dto.setTotalItemsOut(totalOut);
        dto.setTotalRevenue(totalRevenue);
        dto.setStockValue(calculateStockValue());

        saveOrUpdateReport(dto);
        return dto;
    }
    // ... [calculateStockValue, saveOrUpdateReport, refreshStatus methods remain the same] ...

    private void saveOrUpdateReport(StockReportDto dto) {
        StockReportEntity entity = reportRepository
                .findByReportDateAndReportType(dto.getDate(), dto.getReportType())
                .orElse(new StockReportEntity());

        entity.setReportType(dto.getReportType());
        entity.setReportDate(dto.getDate());
        entity.setTotalItemsIn(dto.getTotalItemsIn());
        entity.setTotalItemsOut(dto.getTotalItemsOut());
        entity.setTotalRevenue(dto.getTotalRevenue());
        entity.setStockValue(dto.getStockValue());
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