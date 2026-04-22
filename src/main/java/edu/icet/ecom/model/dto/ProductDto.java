package edu.icet.ecom.model.dto;

import java.util.List;

public class ProductDto {
    public Integer prodeuctId;
    public String productName;
    public String category;

    public Double basePrice;
    public String currency;
    public Double discountPercentage;
    public Double discountedPrice;

    public List<String>availableSizes;
    public List<String>availableColors;
    public String material;
    public String season;

    public Integer totalQuantity;
    public String stockStatus;

    public List<ProductVariantDto> variants;

}
