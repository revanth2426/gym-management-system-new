package com.gym.gymmanagementsystem.controller;

import com.gym.gymmanagementsystem.dto.UserDTO;
import com.gym.gymmanagementsystem.dto.UserResponseDTO;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import com.gym.gymmanagementsystem.dto.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> addUser(@Valid @RequestBody UserDTO userDTO) {
        // Service method now directly accepts UserDTO and handles logic
        User savedUser = userService.addUser(userDTO);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page, // Default to page 0
            @RequestParam(defaultValue = "10") int size) { // Default to 10 records per page
        // Create a Pageable object for sorting by userId descending (newest first, or by name, etc.)
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").descending()); // Sort by userId
        Page<UserResponseDTO> usersPage = userService.getAllUsers(pageable);
        return ResponseEntity.ok(usersPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") String userId) {
        Optional<User> user = userService.getUserById(Integer.parseInt(userId));
        return user.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

@PutMapping("/{id}")
public ResponseEntity<User> updateUser(@PathVariable("id") String userId, @Valid @RequestBody UserDTO userDTO) {
    try {
        // FIX HERE: Parse the String userId from @PathVariable to Integer
        // This aligns with UserService.updateUser now expecting Integer userId.
        User updatedUser = userService.updateUser(Integer.parseInt(userId), userDTO); // <--- MODIFIED LINE
        return ResponseEntity.ok(updatedUser);
    } catch (NumberFormatException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return 400 for invalid ID format
    } catch (RuntimeException e) {
        return ResponseEntity.notFound().build(); // Return 404 if user not found (from service layer exception)
    }
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String userId) {
        try {
            userService.deleteUser(Integer.parseInt(userId));
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

        // MODIFIED: Returns List<UserResponseDTO>
    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String query) {
        List<UserResponseDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
}