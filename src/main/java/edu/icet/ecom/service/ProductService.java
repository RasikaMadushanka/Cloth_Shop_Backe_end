package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.dto.ProductVariantDto;
import edu.icet.ecom.model.entity.ProductEntity;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.repository.ProductRepository;
import edu.icet.ecom.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public void saveProduct(ProductDto productDto) {
        ProductEntity entity = modelMapper.map(productDto, ProductEntity.class);

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                variant.setProduct(entity);
                variant.setSku(generateSku(entity, variant));
                if (variant.getBarcodeId() == null || variant.getBarcodeId().isEmpty()) {
                    variant.setBarcodeId(generateUniqueBarcode());
                }
            });
        }

        refreshProductMetrics(entity);
        productRepository.save(entity);
    }

    @Transactional
    public void updateProduct(ProductDto productDto) {
        ProductEntity existingEntity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Update fields only if they are provided in the DTO
        if (productDto.getProductName() != null) existingEntity.setProductName(productDto.getProductName());
        if (productDto.getCategory() != null) existingEntity.setCategory(productDto.getCategory());
        if (productDto.getBasePrice() != null) existingEntity.setBasePrice(productDto.getBasePrice());
        if (productDto.getCurrency() != null) existingEntity.setCurrency(productDto.getCurrency());
        if (productDto.getDiscountPercentage() != null) existingEntity.setDiscountPercentage(productDto.getDiscountPercentage());
        if (productDto.getMaterial() != null) existingEntity.setMaterial(productDto.getMaterial());
        if (productDto.getSeason() != null) existingEntity.setSeason(productDto.getSeason());

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

    // --- Dynamic Data Mapping (Fixes the Nulls) ---

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Integer id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
        return convertToDto(entity);
    }

    /**
     * Helper to map Entity to DTO and fill transient fields (Colors, Sizes, Totals)
     */
    private ProductDto convertToDto(ProductEntity entity) {
        ProductDto dto = modelMapper.map(entity, ProductDto.class);

        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {
            // 1. Extract Unique Sizes
            dto.setAvailableSizes(entity.getVariants().stream()
                    .map(ProductVariantEntity::getSize)
                    .distinct()
                    .collect(Collectors.toList()));

            // 2. Extract Unique Colors
            dto.setAvailableColors(entity.getVariants().stream()
                    .map(ProductVariantEntity::getColor)
                    .distinct()
                    .collect(Collectors.toList()));

            // 3. Calculate Total Quantity
            int total = entity.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
            dto.setTotalQuantity(total);

            // 4. Calculate Discounted Price
            if (entity.getBasePrice() != null && entity.getDiscountPercentage() != null) {
                double discounted = entity.getBasePrice() * (1 - (entity.getDiscountPercentage() / 100));
                dto.setDiscountedPrice(discounted);
            }
        }
        return dto;
    }

    private void refreshProductMetrics(ProductEntity entity) {
        int totalQty = 0;
        if (entity.getVariants() != null) {
            totalQty = entity.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
        }

        if (totalQty <= 0) {
            entity.setStockStatus(StockStatus.OUT_OF_STOCK);
        } else if (totalQty < 5) {
            entity.setStockStatus(StockStatus.LOW_STOCK);
        } else {
            entity.setStockStatus(StockStatus.AVAILABLE);
        }
    }

    private String generateSku(ProductEntity p, ProductVariantEntity v) {
        String category = (p.getCategory() != null) ? p.getCategory() : "GEN";
        String productName = (p.getProductName() != null) ? p.getProductName() : "PROD";
        String size = (v.getSize() != null) ? v.getSize() : "NOSIZ";

        String catPart = category.substring(0, Math.min(category.length(), 3)).toUpperCase();
        String namePart = productName.substring(0, Math.min(productName.length(), 3)).toUpperCase();

        return (catPart + "-" + namePart + "-" + size).replace(" ", "").toUpperCase();
    }

    private String generateUniqueBarcode() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(4);
        int random = (int) (Math.random() * 900) + 100;
        return timestamp + random;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Delete failed: Product ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }

    public List<String> getAllBarcodeIdsOnly() {
        return variantRepository.findAll()
                .stream()
                .map(ProductVariantEntity::getBarcodeId)
                .filter(id -> id != null)
                .toList();
    }
}