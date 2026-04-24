package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockReportDto {
    private String  reportType; //Daily or Monthly
    private String date; // 2024-04-22
    private Integer totalItemsIn;
    private Integer totalItemsOut;
    private Double totalRevenue;
    private List<LowStockDto>lowStockAlert;
    private Double stockValue;
}

