package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.ProductDto;
import edu.icet.ecom.model.dto.ProductVariantDto;
import edu.icet.ecom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin()

public class ProductController {
    private final ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<String> addProducts(@RequestBody ProductDto productDto) {
        productService.saveProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Product added successfully");
    }

    @GetMapping("/barcodes/list-only")
    public ResponseEntity<List<Map<String, Object>>> getBarcodesWithPrices() {
        return ResponseEntity.ok(productService.getBarcodesAndPrices());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts() { // Changed return type to List
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable Integer id, @RequestBody ProductDto productDto) {
        // Ensure the ID from the path is set into the DTO
        productDto.setProductId(id);
        productService.updateProduct(productDto);
        return ResponseEntity.ok("Product ID: " + id + " updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @PatchMapping("/update-price-by-barcode")
    public ResponseEntity<Map<String, String>> updatePrice(@RequestBody Map<String, Object> request) {
        String barcode = (String) request.get("barcodeId");
        Double price = Double.valueOf(request.get("newPrice").toString());

        productService.updatePriceByBarcode(barcode, price);

        return ResponseEntity.ok(Map.of(
                "message", "Price updated successfully",
                "barcode", barcode,
                "updatedPrice", price.toString()
        ));
    }
}
