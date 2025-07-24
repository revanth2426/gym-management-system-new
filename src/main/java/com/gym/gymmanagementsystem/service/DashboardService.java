package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.model.PlanAssignment;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.repository.PlanAssignmentRepository;
import com.gym.gymmanagementsystem.repository.TrainerRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gym.gymmanagementsystem.dto.ExpiringMembershipDTO; // New import

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private PlanAssignmentRepository planAssignmentRepository;

    @Autowired
    private AttendanceService attendanceService;

    public long getTotalActiveMembers() {
        return userRepository.findAll().stream()
                .filter(user -> "Active".equalsIgnoreCase(user.getMembershipStatus()))
                .count();
    }

    public List<ExpiringMembershipDTO> getMembershipsExpiringSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(days);
        List<PlanAssignment> expiringAssignments = planAssignmentRepository.findByEndDateBetween(today, cutoffDate);

        return expiringAssignments.stream().map(assignment -> {
            ExpiringMembershipDTO dto = new ExpiringMembershipDTO();
            dto.setAssignmentId(assignment.getAssignmentId());
            dto.setUserName(assignment.getUser() != null ? assignment.getUser().getName() : "N/A");
            dto.setPlanName(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanName() : "N/A");
            dto.setEndDate(assignment.getEndDate());
            dto.setUserId(assignment.getUser() != null ? String.valueOf(assignment.getUser().getUserId()) : null);
            dto.setPlanId(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanId() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    public long getTotalTrainers() {
        return trainerRepository.count();
    }

    public Map<String, Long> getPlanDistribution() {
        List<PlanAssignment> activeAssignments = planAssignmentRepository.findAll().stream()
                .filter(assignment -> !assignment.getEndDate().isBefore(LocalDate.now())) // Only count active/future plans
                .collect(Collectors.toList());

        return activeAssignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getMembershipPlan().getPlanName(),
                        Collectors.counting()
                ));
    }

    public Map<LocalDate, Long> getDailyAttendanceData(LocalDate startDate, LocalDate endDate) {
        return attendanceService.getDailyAttendanceCount(startDate, endDate);
    }

    public List<User> searchUsers(String query) {
        // MODIFIED: Handle Integer userId for search
        try {
            // Attempt to parse the query as an Integer for an exact ID match
            Integer userId = Integer.parseInt(query);
            // If parsing succeeds, find by ID. findById returns Optional, map it to a List.
            return userRepository.findById(userId).map(List::of).orElse(List.of());
        } catch (NumberFormatException e) {
            // If the query is not a valid number, fall back to searching by name (case-insensitive)
            return userRepository.findAll().stream()
                    .filter(user -> user.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }
    }

    public List<User> filterUsersByStatus(String status) {
        return userRepository.findByMembershipStatus(status);
    }
}