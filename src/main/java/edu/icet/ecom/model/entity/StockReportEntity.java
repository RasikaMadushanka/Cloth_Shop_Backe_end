package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "stock_report")
public class StockReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    private String reportType;    // "DAILY" or "MONTHLY"
    private String reportDate;    // e.g., "2026-04-23"

    private Integer totalItemsIn;  // Sum of positive quantityChange in StockLog
    private Integer totalItemsOut; // Sum of negative quantityChange in StockLog
    private Double totalRevenue;   // Total sales amount for this period

    private Double stockValue;     // (Current Stock * Price) at the time of report
    private String generatedAt;    // When the report was actually compiled
}