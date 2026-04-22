package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantDto {
    public String VarientId;
    public String size;
    public String color;
    public Integer stockQuantity;
    public String sku;
    public String barcodeId;

    public Double priceOverride;
    public Double finalPrice;
 }
