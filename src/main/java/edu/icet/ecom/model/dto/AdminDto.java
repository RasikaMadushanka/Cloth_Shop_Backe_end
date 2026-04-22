package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDto {
    private Integer adminId;
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String NIC;
    private String Address;
    private String lastLoginTime;
    private Boolean isActive;


}
