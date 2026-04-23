package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.StockLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockLogRepository extends JpaRepository<StockLogEntity, Long> {
    @Query("SELECT s FROM StockLogEntity s WHERE s.timestamp LIKE :pattern%")
    List<StockLogEntity> findByTimestampPattern(@Param("pattern") String pattern);
}