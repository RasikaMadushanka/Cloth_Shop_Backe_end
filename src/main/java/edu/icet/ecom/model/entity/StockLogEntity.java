package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "stock_log", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_daily_restock",
                columnNames = {"barcodeId", "quantityChange", "timestamp"}
        )
})
public class StockLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    // Use MERGE to prevent TransientPropertyValueException when saving logs for new variants
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    private String barcodeId;
    private Integer quantityChange;
    private String updateReason;
    private String timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private AdminEntity admin;
}