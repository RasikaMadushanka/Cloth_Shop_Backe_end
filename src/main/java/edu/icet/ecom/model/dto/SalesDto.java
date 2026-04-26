package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SalesDto {
    private String saleId;
    private Integer adminId;
    private Double totalAmount;
    private Double discountedApplied;
    private Double discountPercentage;
    private Double netAmount;
    private String paymentMethod;
    private String timestamp;
    private List<SalesItemDto> items;
}
