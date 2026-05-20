package com.ecommerce.user_service.controller;


import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://192.168.11.168", "*"})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test endpoint works!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // This endpoint will be used by other services to validate tokens
        // Implementation will be added in JwtAuthFilter
        return ResponseEntity.ok("Token is valid");
    }

}
