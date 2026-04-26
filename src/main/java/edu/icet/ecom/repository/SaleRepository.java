package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity,String > {
    List<SaleEntity> findByTimestampStartingWith(String datePattern);
}
