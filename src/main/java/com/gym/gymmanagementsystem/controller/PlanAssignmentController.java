// src/main/java/com/gym/gymmanagementsystem/controller/PlanAssignmentController.java
package com.gym.gymmanagementsystem.controller;

import com.gym.gymmanagementsystem.service.PlanAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan-assignments") // Dedicated path for plan assignments
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PlanAssignmentController {

    @Autowired
    private PlanAssignmentService planAssignmentService;

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deletePlanAssignment(@PathVariable Integer assignmentId) {
        try {
            planAssignmentService.deletePlanAssignment(assignmentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}