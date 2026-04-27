package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.AdminDto;
import edu.icet.ecom.model.entity.AdminEntity;
import edu.icet.ecom.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService implements UserDetailsService {
    private final ModelMapper modelMapper;
    private final AdminRepository adminRepository;

    public void DeleteAdmin(Integer id) {
        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin with id " + id + " not found");
        }
        adminRepository.deleteById(id);
    }

    public void AddAdmin(AdminDto adminDto) {
        AdminEntity entity = modelMapper.map(adminDto, AdminEntity.class);
        adminRepository.save(entity);

    }

    public void UpdateAdmin(AdminDto adminDto) {
        AdminEntity existingAdmin = adminRepository.findById(adminDto.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminDto.getAdminId()));

        existingAdmin.setUsername(adminDto.getUsername());
        existingAdmin.setPassword(adminDto.getPassword());
        existingAdmin.setRole(adminDto.getRole());
        existingAdmin.setFullName(adminDto.getFullName());
        existingAdmin.setIsActive(adminDto.getIsActive());
        adminRepository.save(existingAdmin);
    }

    public List<AdminDto> getAllAdmins() {
        List<AdminEntity> entities = adminRepository.findAll();

        return entities.stream()
                .map(entity -> modelMapper.map(entity, AdminDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminEntity admin = adminRepository.findByUsername(username);

        if (admin == null) {
            throw new UsernameNotFoundException("Admin not found with username: " + username);
        }

        // Convert your AdminEntity into a Spring Security User object
        return org.springframework.security.core.userdetails.User
                .withUsername(admin.getUsername())
                .password(admin.getPassword())
                .authorities(admin.getRole()) // Ensure this matches your role string (e.g., "ROLE_ADMIN")
                .build();
    }

    @PostConstruct
    public void initDefaultAdmin() {
        // Check if the database is empty or if your specific admin doesn't exist
        if (adminRepository.findByUsername("admin") == null) {
            AdminDto defaultAdmin = new AdminDto();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setPassword("admin123"); // Note: You should ideally encode this
            defaultAdmin.setRole("ADMIN");
            defaultAdmin.setFullName("System Administrator");
            defaultAdmin.setIsActive(true);

            // Use your existing AddAdmin logic
            AddAdmin(defaultAdmin);
            System.out.println(">>> Default Admin 'admin' has been initialized.");
        }
    }
}
