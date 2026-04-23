package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantDto {
    private String variantId; // ✅ Changed from VarientId to variantId
    private String size;
    private String color;
    private Integer stockQuantity;
    private String sku;
    private String barcodeId;
    private Double priceOverride;
    private Double finalPrice;
}