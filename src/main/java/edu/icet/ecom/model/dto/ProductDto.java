package edu.icet.ecom.model.dto;

import edu.icet.ecom.enums.StockStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private StockStatus stockStatus;

    private List<String>availableSizes;
    private List<String>availableColors;
    private String material;
    private String season;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer totalQuantity;
    private List<ProductVariantDto> variants;

}
