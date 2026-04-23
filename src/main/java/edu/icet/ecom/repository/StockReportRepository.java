package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.StockReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockReportRepository extends JpaRepository<StockReportEntity, Long> {

    // Useful if you want to check if a report for a specific date already exists
    Optional<StockReportEntity> findByReportDateAndReportType(String reportDate, String reportType);

    // Useful for cleaning up old reports before re-generating
    void deleteByReportDateAndReportType(String reportDate, String reportType);
}