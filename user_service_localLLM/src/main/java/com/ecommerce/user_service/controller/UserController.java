package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.LocationUpdateRequest;
import com.ecommerce.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://192.168.11.168", "*"})
public class UserController {

    private final AuthService authService;

    @PutMapping("/location")
    public ResponseEntity<Void> updateLocation(@RequestBody LocationUpdateRequest request, Principal principal) {
        authService.updateLocation(principal.getName(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok().build();
    }
}
