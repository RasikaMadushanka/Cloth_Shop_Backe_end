package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantDto {
    private String VarientId;
    private String size;
    private String color;
    private Integer stockQuantity;
    private String sku;
    private String barcodeId;

    private Double priceOverride;
    private Double finalPrice;
 }
