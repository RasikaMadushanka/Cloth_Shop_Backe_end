package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesItemDto {
    private String barcodeId;
    private String  varientId;
    private String itemName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
}
