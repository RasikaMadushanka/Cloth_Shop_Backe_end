package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "admin")

public class AdminEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminId;

    @Column(unique = true)
    private String username;
    private String password; // Should be hashed
    private String role;
    private String fullName;
    private String nic;
    private String address;
    private String lastLoginTime;
    private Boolean isActive;

}
