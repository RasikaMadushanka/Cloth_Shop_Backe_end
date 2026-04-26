package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LowStockDto {
    private String barcodeId;
    private String productName;
    private Integer currentQty;
    private String status; // LOW OR OUT

}
