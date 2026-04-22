package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDto {
    public Integer adminId;
    public String username;
    public String password;
    public String role;
    public String fullName;
    public String NIC;
    public String Address;
    public String lastLoginTime;
    public Boolean isActive;


}
