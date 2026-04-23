package edu.icet.ecom.service;

import edu.icet.ecom.enums.StockStatus;
import edu.icet.ecom.exceptions.ResourceNotFoundException; // Import the custom exception
import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.entity.ProductEntity;
import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public void saveProduct(ProductDto productDto) {
        ProductEntity entity = modelMapper.map(productDto, ProductEntity.class);

        // 1. Calculate Total Quantity across ALL sizes (S, M, 22, 24, etc.)
        int totalQty = entity.getVariants().stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();

        // 2. Set Stock Status Enum
        if (totalQty <= 0) {
            entity.setStockStatus(StockStatus.OUT_OF_STOCK);
        } else if (totalQty < 5) {
            entity.setStockStatus(StockStatus.LOW_STOCK);
        } else {
            entity.setStockStatus(StockStatus.AVAILABLE);
        }

        // 3. Generate Unique IDs for every Size Variant
        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> {
                // Link to parent
                variant.setProduct(entity);

                // Generate Readable SKU: e.g., "TRO-CHI-32"
                variant.setSku(generateSku(entity, variant));

                // Generate Unique Barcode: e.g., "8901713852001"
                if (variant.getBarcodeId() == null || variant.getBarcodeId().isEmpty()) {
                    variant.setBarcodeId(generateUniqueBarcode());
                }
            });
        }

        productRepository.save(entity);
    }

    // Helper: Generates a human-readable code for your staff
    private String generateSku(ProductEntity p, ProductVariantEntity v) {
        String cat = p.getCategory().substring(0, Math.min(p.getCategory().length(), 3)).toUpperCase();
        String name = p.getProductName().substring(0, Math.min(p.getProductName().length(), 3)).toUpperCase();
        // Returns something like "SHI-LIN-XXL" or "TRO-DEN-34"
        return (cat + "-" + name + "-" + v.getSize()).replace(" ", "").toUpperCase();
    }

    // Helper: Generates a 12-digit unique number for the POS scanner
    private String generateUniqueBarcode() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(4); // Last 9 digits of time
        int random = (int) (Math.random() * 900) + 100; // 3 random digits
        return timestamp + random;
    }


    public List<ProductDto> getAllProducts() {
        List<ProductEntity> entities = productRepository.findAll();
        return modelMapper.map(entities, new TypeToken<List<ProductDto>>() {}.getType());
    }

    public ProductDto getProductById(Integer id) {
        return productRepository.findById(id)
                .map(entity -> modelMapper.map(entity, ProductDto.class))
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found"));
    }

    @Transactional
    public void updateProduct(ProductDto productDto) {
        // Fetch existing record to ensure it is managed by Hibernate
        ProductEntity existingEntity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Update failed: Product not found"));

        // Map DTO data onto the existing entity
        modelMapper.map(productDto, existingEntity);

        // Re-link variants to the parent
        if (existingEntity.getVariants() != null) {
            existingEntity.getVariants().forEach(variant -> variant.setProduct(existingEntity));
        }
        productRepository.save(existingEntity);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Delete failed: Product ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }
}