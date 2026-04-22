package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table (name = "sales")
public class SaleEntity {
    @Id
    private String saleId;

    private Double totalAmount;
    private Double netAmount;
    private String paymentMethod;
    private String timestamp;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private AdminEntity admin;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SalesItemEntity> items;
}
