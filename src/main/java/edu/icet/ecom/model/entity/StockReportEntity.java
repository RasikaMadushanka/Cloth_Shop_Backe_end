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

    private String reportType;   // DAILY / MONTHLY / YEARLY
    private String reportDate;   // 2026-04-23

    private Integer totalItemsIn;
    private Integer totalItemsOut;
    private Double totalRevenue;
    @Column(name = "total_discount_given")
    private Double totalDiscountGiven;

    private Double soldItemsValue;

    private Double stockValue;

    private String generatedAt;
}