package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.entity.ProductEntity;
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

        if (entity.getVariants() != null) {
            entity.getVariants().forEach(variant -> variant.setProduct(entity));
        }

        productRepository.save(entity);
    }

    public List<ProductDto> getAllProducts() {
        List<ProductEntity> entities = productRepository.findAll();
        return modelMapper.map(entities, new TypeToken<List<ProductDto>>() {}.getType());
    }

    public ProductDto getProductById(Integer id) {
        return productRepository.findById(id)
                .map(entity -> modelMapper.map(entity, ProductDto.class))
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    @Transactional
    public void updateProduct(ProductDto productDto) {
        // 1. Check if the product exists in the DB
        ProductEntity existingEntity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Update failed: Product ID not found."));

        // 2. Use ModelMapper to map the DTO data INTO the existing managed entity
        // This 'merges' the changes from the DTO into the entity we just found
        modelMapper.map(productDto, existingEntity);

        // 3. Re-link the variants
        // Since we mapped into 'existingEntity', we must ensure its children know who the parent is
        if (existingEntity.getVariants() != null) {
            existingEntity.getVariants().forEach(variant -> variant.setProduct(existingEntity));
        }

        // 4. Save the updated entity
        productRepository.save(existingEntity);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
        } else {
            throw new RuntimeException("Delete failed: Product ID " + id + " does not exist.");
        }
    }
}