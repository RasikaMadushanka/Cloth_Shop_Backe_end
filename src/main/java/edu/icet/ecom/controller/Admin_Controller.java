package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.AdminDto;
import edu.icet.ecom.model.dto.LoginRequestDto;
import edu.icet.ecom.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@CrossOrigin()

public class Admin_Controller {
    private final AdminService adminService;

    @GetMapping("/all")
    public List<AdminDto>getAllAdmins(){
        return adminService.getAllAdmins();
    }

    @PostMapping("/add")
    public ResponseEntity<String> AddAdmin(@RequestBody AdminDto adminDto){
        adminService.AddAdmin(adminDto);
        return ResponseEntity.ok("Admin added successfully");

    }
    @PutMapping("/update/{id}")
        public ResponseEntity<String> UpdateAdmin(@PathVariable Integer id ,@RequestBody AdminDto adminDto){
            adminDto.setAdminId(id);
            adminService.UpdateAdmin(adminDto);
            return ResponseEntity.ok("Admin updated successfully");
        }

    @DeleteMapping("/delete/{id}")
        public ResponseEntity<String> DeleteAdmin(@PathVariable Integer id) {
        adminService.DeleteAdmin(id);
        return ResponseEntity.ok("Admin deleted successfully");
    }
    @PostMapping("/login")
    public ResponseEntity<AdminDto> login(@RequestBody LoginRequestDto request) {

        AdminDto admin = adminService.login(
                request.getUsername(),
                request.getPassword()
        );

        return ResponseEntity.ok(admin);
    }


}
