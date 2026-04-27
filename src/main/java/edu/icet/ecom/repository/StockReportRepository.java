package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.StockReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockReportRepository extends JpaRepository<StockReportEntity, Long> {
    // Changing Optional to List prevents the "2 results returned" crash
    List<StockReportEntity> findByReportDateAndReportType(String reportDate, String reportType);
}