package edu.icet.ecom.repository;

import edu.icet.ecom.model.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<AdminEntity, Integer> {
    Optional<AdminEntity> findByUsername(String username);
}
