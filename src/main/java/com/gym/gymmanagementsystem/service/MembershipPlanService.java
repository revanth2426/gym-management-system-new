package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.dto.MembershipPlanDTO;
import com.gym.gymmanagementsystem.model.MembershipPlan;
import com.gym.gymmanagementsystem.model.PlanAssignment;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.repository.PlanAssignmentRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


//import com.gym.gymmanagementsystem.dto.PlanAssignmentResponseDTO;

@Service
public class MembershipPlanService {

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private PlanAssignmentRepository planAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    public MembershipPlan addPlan(MembershipPlan plan) {
        return planRepository.save(plan);
    }

    public List<MembershipPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public Optional<MembershipPlan> getPlanById(Integer planId) {
        return planRepository.findById(planId);
    }

    public MembershipPlan updatePlan(Integer planId, MembershipPlan planDetails) {
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + planId));

        plan.setPlanName(planDetails.getPlanName());
        plan.setPrice(planDetails.getPrice());
        plan.setDurationMonths(planDetails.getDurationMonths());
        plan.setFeaturesList(planDetails.getFeaturesList());

        return planRepository.save(plan);
    }

    public void deletePlan(Integer planId) {
        planRepository.deleteById(planId);
    }

    // Method to assign a plan to a user
    /*public PlanAssignment assignPlanToUser(String userId, Integer planId, LocalDate startDate) {
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + planId));

        PlanAssignment assignment = new PlanAssignment();
        assignment.setUser(user);
        assignment.setMembershipPlan(plan);
        assignment.setStartDate(startDate);
        assignment.setEndDate(startDate.plusMonths(plan.getDurationMonths()));

        user.setMembershipStatus("Active");
        userRepository.save(user);

        return planAssignmentRepository.save(assignment);
    }*/

    // Method to get all plan assignments for a user
    /*public List<PlanAssignmentResponseDTO> getPlanAssignmentsByUserId(String userId) {
        // This method now returns DTOs instead of entities
        Integer userIdInt = Integer.parseInt(userId);
        List<PlanAssignment> assignments = planAssignmentRepository.findByUser_UserId(userIdInt);

        return assignments.stream().map(assignment -> {
            PlanAssignmentResponseDTO dto = new PlanAssignmentResponseDTO();
            dto.setAssignmentId(assignment.getAssignmentId());
            // Safely access name and planName, assuming they are eagerly loaded by EntityGraph now
            dto.setUserName(assignment.getUser() != null ? assignment.getUser().getName() : "N/A");
            dto.setPlanName(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanName() : "N/A");
            dto.setStartDate(assignment.getStartDate());
            dto.setEndDate(assignment.getEndDate());
            dto.setUserId(assignment.getUser() != null ? String.valueOf(assignment.getUser().getUserId()) : null); // Convert Integer to String
            dto.setPlanId(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanId() : null);
            return dto;
        }).collect(Collectors.toList());
    }*/

    // BEFORE: public MembershipPlan updatePlan(Integer planId, MembershipPlan planDetails) {
public MembershipPlan updatePlan(Integer planId, MembershipPlanDTO planDTO) { // Change parameter to MembershipPlanDTO
    MembershipPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + planId));

    // Copy properties from DTO to entity
    plan.setPlanName(planDTO.getPlanName());
    plan.setPrice(planDTO.getPrice());
    plan.setDurationMonths(planDTO.getDurationMonths());
    plan.setFeaturesList(planDTO.getFeaturesList());

    return planRepository.save(plan);
}

    // Method to get expiring memberships for the dashboard
    public List<PlanAssignment> getExpiringMemberships(int daysBeforeExpiry) {
        LocalDate cutoffDate = LocalDate.now().plusDays(daysBeforeExpiry);
        return planAssignmentRepository.findByEndDateBetween(LocalDate.now(), cutoffDate);
    }
}