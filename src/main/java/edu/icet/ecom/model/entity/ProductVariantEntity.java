package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "product_variant")
@Entity
@Getter
@Setter
public class ProductVariantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String variantId; // e.g., V-5001

    private String size;
    private String color;
    private Integer stockQuantity;
    private String sku;

    @Column(unique = true)
    private String barcodeId; // Scan key

    private Double priceOverride;

    // Many Variants -> One Product
    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;
}
