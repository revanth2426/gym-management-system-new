// src/main/java/com/gym/gymmanagementsystem/dto/UserResponseDTO.java - COMPLETE FILE
package com.gym.gymmanagementsystem.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List; // NEW IMPORT for the list of plans

@Data
public class UserResponseDTO {
    private Integer userId;
    private String name;
    private Integer age;
    private String gender;
    private String contactNumber;
    private String membershipStatus;
    private LocalDate joiningDate;
    // OLD: private String currentPlanName; // This is removed
    // NEW:
    private List<PlanAssignmentDetailDTO> assignedPlans; // List of all plans (active/inactive/expired)
}