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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Updates the stock for a specific variant and logs the transaction.
     * Supports manual dates for backdating reports.
     */
    @Transactional
    public void updateStock(StockUpdateDto stockUpdateDto) {
        ProductVariantEntity variant = variantRepository.findByBarcodeId(stockUpdateDto.getBarcodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Barcode " + stockUpdateDto.getBarcodeId() + " not found"));

        int currentQty = (variant.getStockQuantity() != null) ? variant.getStockQuantity() : 0;
        variant.setStockQuantity(currentQty + stockUpdateDto.getQuantityAdded());
        variantRepository.save(variant);

        StockLogEntity log = new StockLogEntity();
        log.setVariant(variant);
        log.setBarcodeId(variant.getBarcodeId());
        log.setQuantityChange(stockUpdateDto.getQuantityAdded());
        log.setUpdateReason(stockUpdateDto.getUpdateReason());

        // ✅ CHECK: If a date is provided in DTO, use it. Otherwise, use current time.
        if (stockUpdateDto.getDate() != null && !stockUpdateDto.getDate().isEmpty()) {
            log.setTimestamp(stockUpdateDto.getDate());
        } else {
            log.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        logRepository.save(log);
        refreshStatus(variant.getProduct().getProductId());
    }

    /**
     * Generates a summary report AND saves a snapshot to the database.
     */
    @Transactional
    public StockReportDto generateReport(String type, String datePattern) {
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(datePattern);

        StockReportDto reportDto = new StockReportDto();
        reportDto.setReportType(type);
        reportDto.setDate(datePattern);

        int itemsIn = 0;
        int itemsOut = 0;
        double revenue = 0.0;

        for (StockLogEntity log : logs) {
            if (log.getQuantityChange() > 0) {
                itemsIn += log.getQuantityChange();
            } else {
                int soldQty = Math.abs(log.getQuantityChange());
                itemsOut += soldQty;
                double unitPrice = (log.getVariant().getPriceOverride() != null) ?
                        log.getVariant().getPriceOverride() :
                        log.getVariant().getProduct().getBasePrice();
                revenue += (soldQty * unitPrice);
            }
        }

        reportDto.setTotalItemsIn(itemsIn);
        reportDto.setTotalItemsOut(itemsOut);
        reportDto.setTotalRevenue(revenue);

        // ✅ PERSISTENCE: Save this report to the stock_report table
        saveReportToDatabase(reportDto);

        return reportDto;
    }

    /**
     * Helper method to save the report snapshot.
     */
    private void saveReportToDatabase(StockReportDto dto) {
        StockReportEntity reportEntity = new StockReportEntity();
        reportEntity.setReportType(dto.getReportType().toUpperCase());
        reportEntity.setReportDate(dto.getDate());
        reportEntity.setTotalItemsIn(dto.getTotalItemsIn());
        reportEntity.setTotalItemsOut(dto.getTotalItemsOut());
        reportEntity.setTotalRevenue(dto.getTotalRevenue());
        reportEntity.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        reportRepository.save(reportEntity);
    }

    public void refreshStatus(Integer productId) {
        productRepository.findById(productId).ifPresent(product -> {
            int totalStock = product.getVariants().stream()
                    .mapToInt(v -> (v.getStockQuantity() != null) ? v.getStockQuantity() : 0)
                    .sum();

            if (totalStock <= 0) product.setStockStatus(StockStatus.OUT_OF_STOCK);
            else if (totalStock < 10) product.setStockStatus(StockStatus.LOW_STOCK);
            else product.setStockStatus(StockStatus.AVAILABLE);

            productRepository.save(product);
        });
    }
}