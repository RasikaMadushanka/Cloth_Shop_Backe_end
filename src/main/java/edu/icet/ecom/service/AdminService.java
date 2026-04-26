package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.AdminDto;
import edu.icet.ecom.model.entity.AdminEntity;
import edu.icet.ecom.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
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
}

