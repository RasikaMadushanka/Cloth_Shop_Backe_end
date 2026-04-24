package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.StockReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockReportRepository extends JpaRepository<StockReportEntity, Long> {

    Optional<StockReportEntity> findByReportDateAndReportType(String reportDate, String reportType);
}