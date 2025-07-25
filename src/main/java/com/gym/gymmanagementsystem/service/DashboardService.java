package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.model.MembershipPlan;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.repository.TrainerRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest; // NEW IMPORT
import org.springframework.data.domain.Pageable;   // NEW IMPORT
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gym.gymmanagementsystem.dto.ExpiringMembershipDTO;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private AttendanceService attendanceService;

    public long getTotalActiveMembers() {
        return userRepository.findAll().stream()
                .filter(user -> "Active".equalsIgnoreCase(user.getMembershipStatus()) ||
                                (user.getCurrentPlanEndDate() != null && user.getCurrentPlanEndDate().isAfter(LocalDate.now())))
                .count();
    }

    public List<ExpiringMembershipDTO> getMembershipsExpiringSoon(int days) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.plusDays(days);

        return userRepository.findAll().stream()
                .filter(user -> user.getCurrentPlanId() != null && // Must have a plan
                                user.getCurrentPlanEndDate() != null &&
                                !user.getCurrentPlanEndDate().isBefore(today) && // End date is today or in future
                                !user.getCurrentPlanEndDate().isAfter(cutoffDate)) // End date is within the cutoff period
                .map(user -> {
                    ExpiringMembershipDTO dto = new ExpiringMembershipDTO();
                    dto.setUserId(String.valueOf(user.getUserId()));
                    dto.setUserName(user.getName());
                    dto.setPlanId(user.getCurrentPlanId());
                    dto.setEndDate(user.getCurrentPlanEndDate());

                    if (user.getCurrentPlanId() != null) {
                        membershipPlanRepository.findById(user.getCurrentPlanId()).ifPresent(plan -> {
                            dto.setPlanName(plan.getPlanName());
                        });
                    } else {
                        dto.setPlanName("N/A");
                    }
                    return dto;
                }).collect(Collectors.toList());
    }

    public long getTotalTrainers() {
        return trainerRepository.count();
    }

    public Map<String, Long> getPlanDistribution() {
        // Collect active plans from users
        return userRepository.findAll().stream()
                .filter(user -> user.getCurrentPlanId() != null &&
                                user.getCurrentPlanStartDate() != null &&
                                user.getCurrentPlanEndDate() != null &&
                                user.getCurrentPlanEndDate().isAfter(LocalDate.now())) // Only count truly active plans
                .collect(Collectors.groupingBy(
                        user -> {
                            MembershipPlan plan = membershipPlanRepository.findById(user.getCurrentPlanId()).orElse(null);
                            return plan != null ? plan.getPlanName() : "Unknown Plan";
                        },
                        Collectors.counting()
                ));
    }

    public Map<LocalDate, Long> getDailyAttendanceData(LocalDate startDate, LocalDate endDate) {
        return attendanceService.getDailyAttendanceCount(startDate, endDate);
    }

    // MODIFIED: Use UserRepository's findBySearchQuery
    public List<User> searchUsers(String query) {
        // Create a Pageable object for the search query. For a search dropdown,
        // we'll fetch a reasonable number of results (e.g., first 20).
        Pageable pageable = PageRequest.of(0, 20);

        // Call the comprehensive search method from UserRepository.
        // This method is designed to search by name, user ID (as string), or contact number.
        // .getContent() is used because findBySearchQuery returns a Page, and we need the List of User objects.
        return userRepository.findBySearchQuery(query, pageable).getContent();
    }

    public List<User> filterUsersByStatus(String status) {
        return userRepository.findByMembershipStatus(status);
    }
}