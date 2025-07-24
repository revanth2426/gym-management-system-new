// src/main/java/com/gym/gymmanagementsystem/service/UserService.java - COMPLETE FILE (Replace your entire UserService.java with this code)
package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.gym.gymmanagementsystem.dto.UserDTO;
import com.gym.gymmanagementsystem.model.MembershipPlan;
import com.gym.gymmanagementsystem.model.PlanAssignment;
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.repository.PlanAssignmentRepository;
import com.gym.gymmanagementsystem.dto.UserResponseDTO;
import com.gym.gymmanagementsystem.dto.PlanAssignmentDetailDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private PlanAssignmentRepository planAssignmentRepository;

    private final Random random = new Random();

    private Integer generateUniqueUserId() {
        Integer newUserId;
        int maxAttempts = 100;
        int attempts = 0;
        do {
            random.setSeed(System.nanoTime()); // Use nanoTime for better randomness
            newUserId = random.nextInt(900000) + 100000;
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate a unique 6-digit User ID after " + maxAttempts + " attempts.");
            }
        } while (userRepository.existsById(newUserId));
        return newUserId;
    }

    public User addUser(UserDTO userDTO) {
        User user = new User();
        if (userDTO.getUserId() == null) {
            user.setUserId(generateUniqueUserId());
        } else {
            user.setUserId(userDTO.getUserId());
        }
        user.setName(userDTO.getName());
        user.setAge(userDTO.getAge());
        user.setGender(userDTO.getGender());
        user.setContactNumber(userDTO.getContactNumber());
        user.setMembershipStatus(userDTO.getPlanId() != null ? "Active" : (userDTO.getMembershipStatus() != null ? userDTO.getMembershipStatus() : "Inactive"));
        user.setJoiningDate(userDTO.getJoiningDate() != null ? userDTO.getJoiningDate() : LocalDate.now());

        User savedUser = userRepository.save(user);

        if (userDTO.getPlanId() != null) {
            MembershipPlan plan = membershipPlanRepository.findById(userDTO.getPlanId())
                .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + userDTO.getPlanId()));

            PlanAssignment assignment = new PlanAssignment();
            assignment.setUser(savedUser);
            assignment.setMembershipPlan(plan);
            assignment.setStartDate(LocalDate.now()); // Start date is today
            assignment.setEndDate(LocalDate.now().plusMonths(plan.getDurationMonths())); // Calculate end date

            planAssignmentRepository.save(assignment);
        }

        return savedUser;
    }

    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable); // findAll now uses EntityGraph

        return usersPage.map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());
            dto.setContactNumber(user.getContactNumber());
            dto.setMembershipStatus(user.getMembershipStatus());
            dto.setJoiningDate(user.getJoiningDate());

            // Populate all assigned plans for the user
            dto.setAssignedPlans(user.getPlanAssignments().stream()
                .map(assignment -> {
                    PlanAssignmentDetailDTO paDto = new PlanAssignmentDetailDTO();
                    paDto.setAssignmentId(assignment.getAssignmentId());
                    paDto.setPlanId(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanId() : null);
                    paDto.setPlanName(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanName() : "N/A");
                    paDto.setStartDate(assignment.getStartDate());
                    paDto.setEndDate(assignment.getEndDate());
                    // Determine if plan is active based on its dates
                    paDto.setActive((assignment.getStartDate() != null && !assignment.getStartDate().isAfter(LocalDate.now())) &&
                                     (assignment.getEndDate() != null && !assignment.getEndDate().isBefore(LocalDate.now())));
                    return paDto;
                })
                // Sort by active first, then by start date descending
                .sorted((p1, p2) -> {
                    if (p1.isActive() && !p2.isActive()) return -1;
                    if (!p1.isActive() && p2.isActive()) return 1;
                    if (p1.getEndDate() != null && p2.getEndDate() != null) {
                        int endDateCompare = p2.getEndDate().compareTo(p1.getEndDate());
                        if (endDateCompare != 0) return endDateCompare;
                    }
                    if (p1.getStartDate() != null && p2.getStartDate() != null) {
                        return p2.getStartDate().compareTo(p1.getStartDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList()));

            // currentPlanName is no longer a direct field on UserResponseDTO.
            // It's conceptually derived on the frontend or from assignedPlans.
            // REMOVED THIS LINE: dto.setCurrentPlanName(currentPlanName);
            return dto;
        });
    }

    public Optional<User> getUserById(Integer userId) {
        return userRepository.findById(userId); // This uses the EntityGraph version
    }

    // MODIFIED: updateUser to correctly handle membership plan updates and joiningDate sync
    public User updateUser(Integer userId, UserDTO userDTO) {
        User user = userRepository.findById(userId) // Uses @EntityGraph on findById
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        LocalDate oldJoiningDate = user.getJoiningDate(); // Store old joining date for comparison

        // --- Update Basic User Details ---
        user.setName(userDTO.getName());
        user.setAge(userDTO.getAge());
        user.setGender(userDTO.getGender());
        user.setContactNumber(userDTO.getContactNumber());
        // Update joiningDate if it's provided AND different, OR if we're syncing it from a plan below
        if (userDTO.getJoiningDate() != null && !userDTO.getJoiningDate().equals(user.getJoiningDate())) {
            user.setJoiningDate(userDTO.getJoiningDate());
        }
        // Update membershipStatus if provided in DTO. Plan logic below might override.
        if (userDTO.getMembershipStatus() != null && !userDTO.getMembershipStatus().isEmpty()) {
            user.setMembershipStatus(userDTO.getMembershipStatus());
        }

        // --- Handle Membership Plan Update Logic ---
        Optional<MembershipPlan> newPlanOpt = Optional.empty();
        if (userDTO.getPlanId() != null) { // A plan is selected in the form
            newPlanOpt = membershipPlanRepository.findById(userDTO.getPlanId());
            if (newPlanOpt.isEmpty()) {
                throw new RuntimeException("Membership Plan not found with id: " + userDTO.getPlanId());
            }
        }

        Optional<PlanAssignment> currentActiveAssignmentOpt = user.getPlanAssignments().stream()
            .filter(assignment ->
                (assignment.getStartDate() != null && !assignment.getStartDate().isAfter(LocalDate.now())) &&
                (assignment.getEndDate() != null && !assignment.getEndDate().isBefore(LocalDate.now()))
            )
            .findFirst();

        boolean newPlanIsPresent = newPlanOpt.isPresent();
        boolean currentPlanIsActive = currentActiveAssignmentOpt.isPresent();

        Integer oldPlanId = currentPlanIsActive ? currentActiveAssignmentOpt.get().getMembershipPlan().getPlanId() : null;
        Integer newPlanId = newPlanIsPresent ? newPlanOpt.get().getPlanId() : null;

        boolean planChanged = false;
        if (newPlanIsPresent && (!currentPlanIsActive || !oldPlanId.equals(newPlanId))) {
            planChanged = true; // New plan selected and it's different or no active plan
        } else if (!newPlanIsPresent && currentPlanIsActive) {
            planChanged = true; // User explicitly selected "No Plan" but there was an active one
        }

        if (planChanged) {
            // 1. End the previous active plan, if any
            currentActiveAssignmentOpt.ifPresent(assignment -> {
                assignment.setEndDate(LocalDate.now()); // End date becomes today
                planAssignmentRepository.save(assignment); // Save the updated (ended) assignment
            });

            // 2. Create a new assignment if a new plan was selected
            if (newPlanIsPresent) {
                PlanAssignment newAssignment = new PlanAssignment();
                newAssignment.setUser(user);
                newAssignment.setMembershipPlan(newPlanOpt.get());
                newAssignment.setStartDate(LocalDate.now()); // New plan starts today
                newAssignment.setEndDate(LocalDate.now().plusMonths(newPlanOpt.get().getDurationMonths())); // Recalculate end date
                planAssignmentRepository.save(newAssignment); // Save the new assignment

                user.setMembershipStatus("Active"); // Set status to Active
                // Sync user's joiningDate with the start date of the new plan (as requested)
                // Only update if current joiningDate is older or not explicitly set by userDTO
                if (user.getJoiningDate() == null || user.getJoiningDate().isAfter(newAssignment.getStartDate())) {
                     user.setJoiningDate(newAssignment.getStartDate());
                }
            } else {
                // If plan changed to 'No Plan' (and there was an active one)
                user.setMembershipStatus("Inactive"); // Set status to Inactive
                // JoiningDate remains as it was, as it's no longer tied to an active plan start.
            }
        } else { // Plan did NOT change (same active plan or no plan change)
            // If the form sent a different status than current, and no plan changed, apply it
            if (userDTO.getMembershipStatus() != null && !userDTO.getMembershipStatus().equals(user.getMembershipStatus())) {
                user.setMembershipStatus(userDTO.getMembershipStatus());
            }
            // If status is "Active" but no plan is active (e.g. plan expired outside of this edit flow)
            if ("Active".equals(user.getMembershipStatus()) && currentActiveAssignmentOpt.isEmpty()) {
                 user.setMembershipStatus("Inactive");
             }
            // If joiningDate was updated manually in DTO and no plan change occurred, keep that update
            else if (userDTO.getJoiningDate() != null && !userDTO.getJoiningDate().equals(oldJoiningDate)) {
                user.setJoiningDate(userDTO.getJoiningDate());
            }
        }

        return userRepository.save(user);
    }

    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }

    public List<UserResponseDTO> searchUsers(String query) {
        String lowerCaseQuery = query.trim().toLowerCase();

        if (lowerCaseQuery.isEmpty()) {
            return List.of();
        }

        List<User> allUsers = userRepository.findAll(); // Fetches all users for filtering

        List<User> foundUsers = allUsers.stream()
            .filter(user -> {
                boolean matchesName = user.getName().toLowerCase().contains(lowerCaseQuery);
                boolean matchesId = false;
                if (user.getUserId() != null) {
                    String userIdString = String.valueOf(user.getUserId());
                    matchesId = userIdString.contains(lowerCaseQuery);
                }
                boolean matchesContact = false;
                if (user.getContactNumber() != null) {
                    matchesContact = user.getContactNumber().toLowerCase().contains(lowerCaseQuery);
                }
                return matchesName || matchesId || matchesContact;
            })
            .collect(Collectors.toList());

        return foundUsers.stream().map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());
            dto.setContactNumber(user.getContactNumber());
            dto.setMembershipStatus(user.getMembershipStatus());
            dto.setJoiningDate(user.getJoiningDate());

            // Populate all assigned plans for search results
            dto.setAssignedPlans(user.getPlanAssignments().stream()
                .map(assignment -> {
                    PlanAssignmentDetailDTO paDto = new PlanAssignmentDetailDTO();
                    paDto.setAssignmentId(assignment.getAssignmentId());
                    paDto.setPlanId(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanId() : null);
                    paDto.setPlanName(assignment.getMembershipPlan() != null ? assignment.getMembershipPlan().getPlanName() : "N/A");
                    paDto.setStartDate(assignment.getStartDate());
                    paDto.setEndDate(assignment.getEndDate());
                    paDto.setActive((assignment.getStartDate() != null && !assignment.getStartDate().isAfter(LocalDate.now())) &&
                                     (assignment.getEndDate() != null && !assignment.getEndDate().isBefore(LocalDate.now())));
                    return paDto;
                })
                .sorted((p1, p2) -> {
                    if (p1.isActive() && !p2.isActive()) return -1;
                    if (!p1.isActive() && p2.isActive()) return 1;
                    if (p1.getEndDate() != null && p2.getEndDate() != null) {
                        int endDateCompare = p2.getEndDate().compareTo(p1.getEndDate());
                        if (endDateCompare != 0) return endDateCompare;
                    }
                    if (p1.getStartDate() != null && p2.getStartDate() != null) {
                        return p2.getStartDate().compareTo(p1.getStartDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList()));

            // currentPlanName is no longer a direct field on UserResponseDTO.
            // REMOVED THIS LINE: dto.setCurrentPlanName(currentPlanName);

            return dto;
        }).collect(Collectors.toList());
    }
}