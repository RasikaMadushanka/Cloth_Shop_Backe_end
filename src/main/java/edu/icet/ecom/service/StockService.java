package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.StockReportDto;
import edu.icet.ecom.model.dto.StockUpdateDto;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.StockLogRepository;
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

    /**
     * Updates the stock for a specific variant and logs the transaction.
     */
    @Transactional
    public void updateStock(StockUpdateDto stockUpdateDto) {
        // 1. Find the variant by barcode
        ProductVariantEntity variant = variantRepository.findByBarcodeId(stockUpdateDto.getBarcodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Barcode " + stockUpdateDto.getBarcodeId() + " not found"));

        // 2. Update the current stock quantity
        int currentQty = (variant.getStockQuantity() != null) ? variant.getStockQuantity() : 0;
        variant.setStockQuantity(currentQty + stockUpdateDto.getQuantityAdded());
        variantRepository.save(variant);

        // 3. Create a record in the stock log for reporting
        StockLogEntity log = new StockLogEntity();
        log.setVariant(variant);
        log.setBarcodeId(variant.getBarcodeId());
        log.setQuantityChange(stockUpdateDto.getQuantityAdded());
        log.setUpdateReason(stockUpdateDto.getUpdateReason());
        log.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logRepository.save(log);

        // 4. Update the overall product status (Available, Low Stock, etc.)
        refreshStatus(variant.getProduct().getProductId());
    }

    /**
     * Generates a summary report based on a date pattern (Daily, Monthly, or Yearly).
     */
    public StockReportDto generateReport(String type, String datePattern) {
        List<StockLogEntity> logs = logRepository.findByTimestampPattern(datePattern);

        StockReportDto report = new StockReportDto();
        report.setReportType(type);
        report.setDate(datePattern);

        int itemsIn = 0;
        int itemsOut = 0;
        double revenue = 0.0;

        for (StockLogEntity log : logs) {
            if (log.getQuantityChange() > 0) {
                // Stock increase (Restock/Initial)
                itemsIn += log.getQuantityChange();
            } else {
                // Stock decrease (Sales)
                int soldQty = Math.abs(log.getQuantityChange());
                itemsOut += soldQty;

                // Calculate revenue: Use Variant Price Override if available, else use Product Base Price
                double unitPrice = (log.getVariant().getPriceOverride() != null) ?
                        log.getVariant().getPriceOverride() :
                        log.getVariant().getProduct().getBasePrice();

                revenue += (soldQty * unitPrice);
            }
        }

        report.setTotalItemsIn(itemsIn);
        report.setTotalItemsOut(itemsOut);
        report.setTotalRevenue(revenue);

        return report;
    }

    /**
     * Recalculates the stock status of a product based on the sum of its variants.
     */
    public void refreshStatus(Integer productId) {
        productRepository.findById(productId).ifPresent(product -> {
            int totalStock = product.getVariants().stream()
                    .mapToInt(v -> (v.getStockQuantity() != null) ? v.getStockQuantity() : 0)
                    .sum();

            if (totalStock <= 0) {
                product.setStockStatus(StockStatus.OUT_OF_STOCK);
            } else if (totalStock < 10) {
                product.setStockStatus(StockStatus.LOW_STOCK);
            } else {
                product.setStockStatus(StockStatus.AVAILABLE);
            }
            productRepository.save(product);
        });
    }
}