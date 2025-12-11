package org.example.userservice.service;

import org.example.userservice.dto.UserResponseDTO;
import org.example.userservice.dto.UserStorageInfo;
import org.example.userservice.dto.UserUpdateRequest;
import org.example.userservice.entity.User;
import org.example.userservice.exception.EmailAlreadyExistException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserResponseDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        return userMapper.toResponseDTO(user);
    }

    public UserResponseDTO getProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        return userMapper.toResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO updateProfile(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        // Update first name if provided
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }

        // Update last name if provided
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        // Update email if provided and different from current
        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(email)) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistException("Cet email est déjà utilisé");
            }
            user.setEmail(request.getEmail());
        }

        // Update password if provided
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return userMapper.toResponseDTO(updatedUser);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        userRepository.delete(user);
    }

    @Transactional
    public void updateStorageUsed(Long userId, Long sizeChange) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        
        long newStorageUsed = user.getStorageUsed() + sizeChange;
        if (newStorageUsed < 0) {
            newStorageUsed = 0;
        }
        user.setStorageUsed(newStorageUsed);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean hasStorageSpace(Long userId, Long fileSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        return (user.getStorageUsed() + fileSize) <= user.getMaxStorage();
    }

    public UserStorageInfo getStorageInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        
        long storageUsed = user.getStorageUsed();
        long maxStorage = user.getMaxStorage();
        long availableStorage = maxStorage - storageUsed;
        double storageUsedPercentage = maxStorage > 0 ? (double) storageUsed / maxStorage * 100 : 0;
        
        return UserStorageInfo.builder()
                .storageUsed(storageUsed)
                .maxStorage(maxStorage)
                .availableStorage(availableStorage)
                .storageUsedPercentage(storageUsedPercentage)
                .build();
    }
}
