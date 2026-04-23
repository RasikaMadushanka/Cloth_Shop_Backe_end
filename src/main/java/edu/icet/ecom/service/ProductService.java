package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.dto.ProductVariantDto;
import edu.icet.ecom.model.entity.ProductEntity;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import edu.icet.ecom.repository.StockLogRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final StockLogRepository logRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public void saveProduct(ProductDto productDto) {
        ProductEntity entity = modelMapper.map(productDto, ProductEntity.class);

        // Manual Timestamp Setting
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                variant.setProduct(entity);
                // Ensure a Variant ID exists for the Stock Log relationship
                if (variant.getVariantId() == null) {
                    variant.setVariantId(UUID.randomUUID().toString());
                }
                variant.setSku(generateSku(entity, variant));
                if (variant.getBarcodeId() == null || variant.getBarcodeId().isEmpty()) {
                    variant.setBarcodeId(generateUniqueBarcode());
                }
            });
        }

        refreshProductMetrics(entity);
        productRepository.save(entity);

        // Trigger initial stock logs so reports are populated
        createInitialStockLogs(entity);
    }

    @Transactional
    public void updateProduct(ProductDto productDto) {
        ProductEntity existingEntity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        existingEntity.setUpdatedAt(LocalDateTime.now());

        if (productDto.getProductName() != null) existingEntity.setProductName(productDto.getProductName());
        if (productDto.getCategory() != null) existingEntity.setCategory(productDto.getCategory());
        if (productDto.getBasePrice() != null) existingEntity.setBasePrice(productDto.getBasePrice());

        if (productDto.getVariants() != null) {
            for (ProductVariantDto vDto : productDto.getVariants()) {
                if (vDto.getVariantId() != null && !vDto.getVariantId().isEmpty()) {
                    existingEntity.getVariants().stream()
                            .filter(v -> v.getVariantId().equals(vDto.getVariantId()))
                            .findFirst()
                            .ifPresent(existingVar -> modelMapper.map(vDto, existingVar));
                } else {
                    ProductVariantEntity newVar = modelMapper.map(vDto, ProductVariantEntity.class);
                    newVar.setProduct(existingEntity);
                    newVar.setSku(generateSku(existingEntity, newVar));
                    if (newVar.getBarcodeId() == null) newVar.setBarcodeId(generateUniqueBarcode());
                    existingEntity.getVariants().add(newVar);
                }
            }
        }

        refreshProductMetrics(existingEntity);
        productRepository.save(existingEntity);
    }

    private void createInitialStockLogs(ProductEntity entity) {
        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                StockLogEntity log = new StockLogEntity();
                log.setVariant(variant);
                log.setBarcodeId(variant.getBarcodeId());
                log.setQuantityChange(variant.getStockQuantity());
                log.setUpdateReason("INITIAL_STOCK_ADD");
                log.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                logRepository.save(log);
            });
        }
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public ProductDto getProductById(Integer id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product ID " + id + " not found"));
        return convertToDto(entity);
    }

    private ProductDto convertToDto(ProductEntity entity) {
        ProductDto dto = modelMapper.map(entity, ProductDto.class);
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            dto.setAvailableSizes(entity.getVariants().stream().map(ProductVariantEntity::getSize).distinct().toList());
            dto.setAvailableColors(entity.getVariants().stream().map(ProductVariantEntity::getColor).distinct().toList());
            dto.setTotalQuantity(entity.getVariants().stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum());

            if (entity.getBasePrice() != null && entity.getDiscountPercentage() != null) {
                dto.setDiscountedPrice(entity.getBasePrice() * (1 - (entity.getDiscountPercentage() / 100)));
            }
        }
        return dto;
    }

    private void refreshProductMetrics(ProductEntity entity) {
        int totalQty = (entity.getVariants() != null) ?
                entity.getVariants().stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum() : 0;

        if (totalQty <= 0) entity.setStockStatus(StockStatus.OUT_OF_STOCK);
        else if (totalQty < 10) entity.setStockStatus(StockStatus.LOW_STOCK);
        else entity.setStockStatus(StockStatus.AVAILABLE);
    }

    private String generateSku(ProductEntity p, ProductVariantEntity v) {
        String cat = (p.getCategory() != null) ? p.getCategory().substring(0, Math.min(p.getCategory().length(), 3)) : "GEN";
        String name = (p.getProductName() != null) ? p.getProductName().substring(0, Math.min(p.getProductName().length(), 3)) : "PRD";
        return (cat + "-" + name + "-" + (v.getSize() != null ? v.getSize() : "NA")).toUpperCase().replace(" ", "");
    }

    private String generateUniqueBarcode() {
        return String.valueOf(System.currentTimeMillis()).substring(6) + ((int) (Math.random() * 900) + 100);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (!productRepository.existsById(id)) throw new ResourceNotFoundException("Product not found");
        productRepository.deleteById(id);
    }

    public List<String> getAllBarcodeIdsOnly() {
        return variantRepository.findAll().stream().map(ProductVariantEntity::getBarcodeId).toList();
    }
}