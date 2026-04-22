package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockUpdateDto {
    private String varientId;
    private String barcodeId;
    private Integer quantityAdded;
    private String updateReason; // e.g., "Restock", "Correction", "Return"
    private String date;
}
