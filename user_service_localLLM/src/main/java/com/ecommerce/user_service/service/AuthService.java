package com.ecommerce.user_service.service;


import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthResponse register(RegisterRequest request){
        log.info("Register request received for username: {}", request.getUsername());
        
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        log.info("Creating new user...");
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .role(User.Role.USER)
                .build();

        log.info("Saving user to database...");
        User savedUser = userRepository.save(user);
        log.info("User saved successfully with ID: {}", savedUser.getId());

        log.info("Generating JWT token...");
        String token = jwtService.generateToken(savedUser);

        log.info("Building auth response...");
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .latitude(savedUser.getLatitude())
                .longitude(savedUser.getLongitude())
                .build();
        
        log.info("Register operation completed successfully");
        return response;
    }


    public AuthResponse login(LoginRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }

    public void updateLocation(String username, Double latitude, Double longitude) {
        log.info("Updating location for user: {}, Lat: {}, Lon: {}", username, latitude, longitude);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        userRepository.save(user);
        log.info("Location updated successfully for user: {}", username);
    }

}
