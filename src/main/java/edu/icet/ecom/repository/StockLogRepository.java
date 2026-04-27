package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.ProductVariantEntity;
import edu.icet.ecom.model.entity.StockLogEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockLogRepository extends JpaRepository<StockLogEntity, Long> {

    // This native query or JPQL is required to match "2026-04-24"
    // against a full timestamp like "2026-04-24 10:15:30"
    @Query("SELECT s FROM StockLogEntity s WHERE s.timestamp LIKE CONCAT(:date, '%')")
    List<StockLogEntity> findByTimestampPattern(@Param("date") String date);
    @Modifying
    @Transactional
    void deleteByVariant(ProductVariantEntity variant);
}