package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, String> {
    Optional<ProductVariantEntity> findByBarcodeId(String barcodeId);
}