package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "product")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    private String productName;
    private String category;
    private Double basePrice;
    private String currency;
    private Double discountPercentage;
    private String material;
    private String season;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariantEntity>Variants;


}

