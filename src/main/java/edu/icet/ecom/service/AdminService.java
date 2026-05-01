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
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService implements UserDetailsService {
    private final ModelMapper modelMapper;
    private final AdminRepository adminRepository;

    private String getCurrentTimestamp() {
        return ZonedDateTime.now(ZoneId.of("Asia/Colombo"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void DeleteAdmin(Integer id) {
        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin not found");
        }
        adminRepository.deleteById(id);
    }

    public void AddAdmin(AdminDto adminDto) {
        AdminEntity entity = modelMapper.map(adminDto, AdminEntity.class);
        adminRepository.save(entity);
    }

    public void UpdateAdmin(AdminDto adminDto) {
        AdminEntity existingAdmin = adminRepository.findById(adminDto.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        existingAdmin.setUsername(adminDto.getUsername());
        existingAdmin.setPassword(adminDto.getPassword());
        existingAdmin.setRole(adminDto.getRole());
        existingAdmin.setFullName(adminDto.getFullName());
        existingAdmin.setIsActive(adminDto.getIsActive());
        existingAdmin.setNIC(adminDto.getNIC());
        existingAdmin.setAddress(adminDto.getAddress());

        // Manual update of time if provided from frontend
        existingAdmin.setLastLoginTime(adminDto.getLastLoginTime());

        adminRepository.save(existingAdmin);
    }

    public List<AdminDto> getAllAdmins() {
        List<AdminEntity> entities = adminRepository.findAll();
        return entities.stream()
                .map(entity -> modelMapper.map(entity, AdminDto.class))
                .collect(Collectors.toList());
    }

    /**
     * CRITICAL FIX: Added @Transactional and saveAndFlush
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminEntity admin = adminRepository.findByUsername(username);

        if (admin == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Set the Sri Lanka Local Time
        admin.setLastLoginTime(getCurrentTimestamp());

        // Use saveAndFlush to force the update to the DB immediately
        adminRepository.saveAndFlush(admin);

        return org.springframework.security.core.userdetails.User
                .withUsername(admin.getUsername())
                .password(admin.getPassword())
                .authorities(admin.getRole())
                .build();
    }

    @PostConstruct
    public void initDefaultAdmin() {
        if (adminRepository.findByUsername("SuperAdmin") == null) {
            AdminDto defaultAdmin = new AdminDto();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setNIC("200535100316");
            defaultAdmin.setPassword("admin123");
            defaultAdmin.setRole("SUPER_ADMIN");
            defaultAdmin.setFullName("System Administrator");
            defaultAdmin.setIsActive(true);
            AddAdmin(defaultAdmin);
        }
    }
    public AdminDto login(String username, String password) {

        AdminEntity admin = adminRepository.findByUsername(username);

        // ❌ user not found
        if (admin == null) {
            throw new RuntimeException("Invalid username or password");
        }

        // ❌ wrong password
        if (!admin.getPassword().equals(password)) {
            throw new RuntimeException("Invalid username or password");
        }

        // ✅ update last login time
        admin.setLastLoginTime(getCurrentTimestamp());
        adminRepository.save(admin);

        // ✅ return admin data (mapped to DTO)
        return modelMapper.map(admin, AdminDto.class);
    }
}