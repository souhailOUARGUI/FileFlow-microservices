package org.example.userservice.service;

import org.example.userservice.dto.*;
import org.example.userservice.entity.User;
import org.example.userservice.exception.*;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistException("Cet email est déjà utilisé");
        }

        // Check if email is verified
        if (!emailVerificationService.isEmailVerified(request.getEmail())) {
            throw new EmailNotVerifiedException("Veuillez d'abord vérifier votre email");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStorageUsed(0L);
        user.setMaxStorage(5L * 1024 * 1024 * 1024); // 5GB default

        User savedUser = userRepository.save(user);

        // Remove verified email from cache
        emailVerificationService.removeVerifiedEmail(request.getEmail());

        // Generate tokens
        String accessToken = jwtUtil.generateToken(savedUser.getEmail());

        return new AuthResponse(accessToken, userMapper.toDTO(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Email ou mot de passe incorrect");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Veuillez vérifier votre email avant de vous connecter");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(accessToken, userMapper.toDTO(user));
    }

    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        return userMapper.toDTO(user);
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        return refreshTokenService.refreshAccessToken(refreshToken);
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
        refreshTokenService.deleteByUser(user);
    }
}
