package com.pnpn.userservice.controller;

import com.pnpn.shared.dto.UpdateProfileRequest;
import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.exception.PnPnException;
import com.pnpn.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(userService.getUserById(getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserDto> updateProfile(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = getCurrentUserId();
        if (!currentUserId.equals(id)) {
            throw new PnPnException("You can only update your own profile", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    @GetMapping("/{id}/reputation")
    public ResponseEntity<UserDto> getReputation(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getReputation(id));
    }

    @GetMapping("/{id}/followers/count")
    public ResponseEntity<Map<String, Object>> getFollowerCount(@PathVariable UUID id) {
        int count = userService.getFollowerCount(id);
        return ResponseEntity.ok(Map.of(
                "userId", id,
                "followerCount", count
        ));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new PnPnException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return (UUID) authentication.getPrincipal();
    }
}
