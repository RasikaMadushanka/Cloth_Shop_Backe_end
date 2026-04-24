package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, String> {

    Optional<ProductVariantEntity> findByBarcodeId(String barcodeId);

    // ✅ FIXED METHOD (THIS IS REQUIRED)
    List<ProductVariantEntity> findByProduct_ProductId(Integer productId);
}