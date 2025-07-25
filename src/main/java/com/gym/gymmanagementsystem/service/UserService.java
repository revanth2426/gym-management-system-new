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
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.dto.UserResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    private final Random random = new Random();

    private Integer generateUniqueUserId() {
        Integer newUserId;
        int maxAttempts = 100;
        int attempts = 0;
        do {
            random.setSeed(System.nanoTime());
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
        user.setUserId(userDTO.getUserId() == null ? generateUniqueUserId() : userDTO.getUserId());
        user.setName(userDTO.getName());
        user.setAge(userDTO.getAge());
        user.setGender(userDTO.getGender());
        user.setContactNumber(userDTO.getContactNumber());
        user.setJoiningDate(userDTO.getJoiningDate() != null ? userDTO.getJoiningDate() : LocalDate.now());

        if (userDTO.getSelectedPlanId() != null) {
            MembershipPlan plan = membershipPlanRepository.findById(userDTO.getSelectedPlanId())
                .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + userDTO.getSelectedPlanId()));

            user.setCurrentPlanId(plan.getPlanId());
            user.setCurrentPlanStartDate(user.getJoiningDate());
            user.setCurrentPlanEndDate(user.getJoiningDate().plusMonths(plan.getDurationMonths()));
        } else {
            user.setCurrentPlanId(null);
            user.setCurrentPlanStartDate(null);
            user.setCurrentPlanEndDate(null);
        }
        User savedUser = userRepository.save(user);

        return deriveAndSetUserStatus(savedUser);
    }

    private User deriveAndSetUserStatus(User user) {
        if (user.getCurrentPlanId() != null && user.getCurrentPlanEndDate() != null) {
            if (user.getCurrentPlanEndDate().isAfter(LocalDate.now())) {
                user.setMembershipStatus("Active");
            } else {
                user.setMembershipStatus("Expired");
            }
        } else {
            user.setMembershipStatus("Inactive");
        }
        return user;
    }


    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);

        return usersPage.map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());
            dto.setContactNumber(user.getContactNumber());
            dto.setJoiningDate(user.getJoiningDate());

            if (user.getCurrentPlanId() != null && user.getCurrentPlanStartDate() != null && user.getCurrentPlanEndDate() != null) {
                MembershipPlan plan = membershipPlanRepository.findById(user.getCurrentPlanId()).orElse(null);
                if (plan != null) {
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName(plan.getPlanName());
                    dto.setCurrentPlanStartDate(user.getCurrentPlanStartDate());
                    dto.setCurrentPlanEndDate(user.getCurrentPlanEndDate());

                    boolean isActive = user.getCurrentPlanEndDate().isAfter(LocalDate.now());
                    dto.setCurrentPlanIsActive(isActive);

                    if (isActive) {
                        dto.setMembershipStatus("Active");
                    } else {
                        dto.setMembershipStatus("Expired");
                    }
                } else {
                    dto.setMembershipStatus("Inactive");
                    dto.setCurrentPlanIsActive(false);
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName("Unknown Plan");
                    dto.setCurrentPlanStartDate(null);
                    dto.setCurrentPlanEndDate(null);
                }
            } else {
                dto.setMembershipStatus("Inactive");
                dto.setCurrentPlanIsActive(false);
                dto.setCurrentPlanId(null);
                dto.setCurrentPlanName(null);
                dto.setCurrentPlanStartDate(null);
                dto.setCurrentPlanEndDate(null);
            }
            return dto;
        });
    }

    public Optional<UserResponseDTO> getUserById(Integer userId) {
        return userRepository.findById(userId).map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());
            dto.setContactNumber(user.getContactNumber());
            dto.setJoiningDate(user.getJoiningDate());

            if (user.getCurrentPlanId() != null && user.getCurrentPlanStartDate() != null && user.getCurrentPlanEndDate() != null) {
                MembershipPlan plan = membershipPlanRepository.findById(user.getCurrentPlanId()).orElse(null);
                if (plan != null) {
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName(plan.getPlanName());
                    dto.setCurrentPlanStartDate(user.getCurrentPlanStartDate());
                    dto.setCurrentPlanEndDate(user.getCurrentPlanEndDate());

                    boolean isActive = user.getCurrentPlanEndDate().isAfter(LocalDate.now());
                    dto.setCurrentPlanIsActive(isActive);

                    if (isActive) {
                        dto.setMembershipStatus("Active");
                    } else {
                        dto.setMembershipStatus("Expired");
                    }
                } else {
                    dto.setMembershipStatus("Inactive");
                    dto.setCurrentPlanIsActive(false);
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName("Unknown Plan");
                    dto.setCurrentPlanStartDate(null);
                    dto.setCurrentPlanEndDate(null);
                }
            } else {
                dto.setMembershipStatus("Inactive");
                dto.setCurrentPlanIsActive(false);
                dto.setCurrentPlanId(null);
                dto.setCurrentPlanName(null);
                dto.setCurrentPlanStartDate(null);
                dto.setCurrentPlanEndDate(null);
            }
            return dto;
        });
    }

    public User updateUser(Integer userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setName(userDTO.getName());
        user.setAge(userDTO.getAge());
        user.setGender(userDTO.getGender());
        user.setContactNumber(userDTO.getContactNumber());
        user.setJoiningDate(userDTO.getJoiningDate());

        if (userDTO.getSelectedPlanId() != null) {
            MembershipPlan newPlan = membershipPlanRepository.findById(userDTO.getSelectedPlanId())
                .orElseThrow(() -> new RuntimeException("Membership Plan not found with id: " + userDTO.getSelectedPlanId()));

            boolean hasCurrentlyActiveStoredPlan = (user.getCurrentPlanStartDate() != null && !user.getCurrentPlanStartDate().isAfter(LocalDate.now())) &&
                                                   (user.getCurrentPlanEndDate() != null && user.getCurrentPlanEndDate().isAfter(LocalDate.now()));

            if (hasCurrentlyActiveStoredPlan && !userDTO.getSelectedPlanId().equals(user.getCurrentPlanId())) {
                String currentPlanName = membershipPlanRepository.findById(user.getCurrentPlanId()).map(MembershipPlan::getPlanName).orElse("Unknown Plan");
                throw new RuntimeException("User already has an active membership plan ('" + currentPlanName + "'). Please remove the current plan before assigning a new one.");
            }

            user.setCurrentPlanId(newPlan.getPlanId());
            user.setCurrentPlanStartDate(user.getJoiningDate());
            user.setCurrentPlanEndDate(user.getJoiningDate().plusMonths(newPlan.getDurationMonths()));
        } else {
            user.setCurrentPlanId(null);
            user.setCurrentPlanStartDate(null);
            user.setCurrentPlanEndDate(null);
        }

        return deriveAndSetUserStatus(userRepository.save(user));
    }

    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }

    // MODIFIED: searchUsers now uses the combined search query
    public Page<UserResponseDTO> searchUsers(String query, Pageable pageable) {
        Page<User> usersPage;

        // If query is empty, treat it as a generic findAll for the main table view
        if (query == null || query.trim().isEmpty()) {
            usersPage = userRepository.findAll(pageable);
        } else {
            // Use the new combined search query
            usersPage = userRepository.findBySearchQuery(query.trim(), pageable);
        }

        return usersPage.map(user -> {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());
            dto.setContactNumber(user.getContactNumber());
            dto.setJoiningDate(user.getJoiningDate());

            if (user.getCurrentPlanId() != null && user.getCurrentPlanStartDate() != null && user.getCurrentPlanEndDate() != null) {
                MembershipPlan plan = membershipPlanRepository.findById(user.getCurrentPlanId()).orElse(null);
                if (plan != null) {
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName(plan.getPlanName());
                    dto.setCurrentPlanStartDate(user.getCurrentPlanStartDate());
                    dto.setCurrentPlanEndDate(user.getCurrentPlanEndDate());

                    boolean isActive = user.getCurrentPlanEndDate().isAfter(LocalDate.now());
                    dto.setCurrentPlanIsActive(isActive);

                    if (isActive) {
                        dto.setMembershipStatus("Active");
                    } else {
                        dto.setMembershipStatus("Expired");
                    }
                } else {
                    dto.setMembershipStatus("Inactive");
                    dto.setCurrentPlanIsActive(false);
                    dto.setCurrentPlanId(user.getCurrentPlanId());
                    dto.setCurrentPlanName("Unknown Plan");
                    dto.setCurrentPlanStartDate(null);
                    dto.setCurrentPlanEndDate(null);
                }
            } else {
                dto.setMembershipStatus("Inactive");
                dto.setCurrentPlanIsActive(false);
                dto.setCurrentPlanId(null);
                dto.setCurrentPlanName(null);
                dto.setCurrentPlanStartDate(null);
                dto.setCurrentPlanEndDate(null);
            }
            return dto;
        });
    }
}