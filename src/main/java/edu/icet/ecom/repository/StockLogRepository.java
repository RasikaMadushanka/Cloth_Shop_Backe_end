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
    // Newest logs at the top
    List<StockLogEntity> findAllByOrderByLogIdDesc();

    @Query("SELECT s FROM StockLogEntity s WHERE s.timestamp LIKE CONCAT(:date, '%')")
    List<StockLogEntity> findByTimestampPattern(@Param("date") String date);

    @Modifying
    @Transactional
    void deleteByVariant(ProductVariantEntity variant);
}