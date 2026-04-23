package edu.icet.ecom.service;

import edu.icet.ecom.exceptions.ResourceNotFoundException; // Import the custom exception
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