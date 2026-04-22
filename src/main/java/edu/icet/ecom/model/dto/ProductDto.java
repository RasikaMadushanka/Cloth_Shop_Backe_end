package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductDto {
    private Integer productId;
    private String productName;
    private String category;

    private Double basePrice;
    private String currency;
    private Double discountPercentage;
    private Double discountedPrice;

    private List<String>availableSizes;
    private List<String>availableColors;
    private String material;
    private String season;

    private Integer totalQuantity;
    private String stockStatus;

    private List<ProductVariantDto> variants;

}
