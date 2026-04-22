package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table (name = "stock_log")
public class StockLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    private String barcodeId;      // Snapshot of the barcode used
    private Integer quantityChange; // + for restock, - for sale/damage
    private String updateReason;    // "Restock", "Sale", "Return", "Damage"
    private String timestamp;       // e.g., "2026-04-23 10:30:00"

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private AdminEntity admin;

}
