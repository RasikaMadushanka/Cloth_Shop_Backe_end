package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "sales_item")
public class SalesItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    private SaleEntity sale;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariantEntity variant;
    @Column(name = "discount_amount")
    private Double discountAmount;
    @Column(name = "net_price")
    private Double netPrice;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;



}
