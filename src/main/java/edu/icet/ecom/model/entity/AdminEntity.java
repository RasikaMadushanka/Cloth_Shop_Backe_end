package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "admin")
public class AdminEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminId;

    @Column(unique = true)
    private String username;

    private String password;
    private String role; // e.g. "ROLE_ADMIN"
    private String fullName;
    private Boolean isActive;
    private String NIC;
    private String Address;
    private String lastLoginTime;


}
