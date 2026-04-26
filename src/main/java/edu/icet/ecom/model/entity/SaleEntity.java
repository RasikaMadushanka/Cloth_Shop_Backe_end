package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "sales")
public class SaleEntity {
    @Id
    private String saleId;

    private Double totalAmount;        // Gross total (sum of unit price * qty)
    private Double discountPercentage;  // The % you input manually (e.g., 5.0)
    private Double discountAmount;      // The actual value subtracted (e.g., 150.0)
    private Double netAmount;           // The final bill (totalAmount - discountAmount)

    private String paymentMethod;
    private String timestamp;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private AdminEntity admin;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SalesItemEntity> items;
}