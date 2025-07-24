// src/main/java/com/gym/gymmanagementsystem/dto/PlanAssignmentDetailDTO.java
package com.gym.gymmanagementsystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PlanAssignmentDetailDTO {
    private Integer assignmentId; // Crucial for deletion
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer planId; // The ID of the assigned plan (for reference if needed)
    private boolean isActive; // To indicate if this specific assignment is currently active
}