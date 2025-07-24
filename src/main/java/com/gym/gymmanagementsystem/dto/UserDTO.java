package com.gym.gymmanagementsystem.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // Import NotNull
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

@Data
public class UserDTO {
    private Integer userId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Age is required")
    private Integer age;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits")
    private String contactNumber;

    private String membershipStatus;

    @PastOrPresent(message = "Joining date cannot be in the future")
    @NotNull(message = "Joining date is required") // Made joining date required
    private LocalDate joiningDate;

    // NEW: Make planId mandatory if a plan must always be assigned on user creation
    @NotNull(message = "Membership Plan is required")
    private Integer planId;
}