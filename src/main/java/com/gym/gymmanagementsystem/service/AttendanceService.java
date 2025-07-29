package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.model.Attendance;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.AttendanceRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gym.gymmanagementsystem.dto.AttendanceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;

    private AttendanceResponseDTO convertToDto(Attendance attendance) {
        AttendanceResponseDTO dto = new AttendanceResponseDTO();
        dto.setAttendanceId(attendance.getAttendanceId());
        dto.setUserId(attendance.getUser() != null ? attendance.getUser().getUserId() : null);
        dto.setUserName(attendance.getUser() != null ? attendance.getUser().getName() : "N/A");
        dto.setCheckInTime(attendance.getCheckInTime());
        dto.setCheckOutTime(attendance.getCheckOutTime());
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            Duration duration = Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
            dto.setTimeSpentMinutes(duration.toMinutes());
        } else {
            dto.setTimeSpentMinutes(null);
        }
        return dto;
    }

    public Optional<AttendanceResponseDTO> getAttendanceStatusForToday(Integer userId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByUserUserIdAndAttendanceDate(userId, today)
                .map(this::convertToDto);
    }

    @Transactional
    public AttendanceResponseDTO recordOrUpdateAttendance(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // NEW VALIDATION: Check user's membership status before allowing attendance
        if (!"Active".equalsIgnoreCase(user.getMembershipStatus()) && 
            (user.getCurrentPlanEndDate() == null || user.getCurrentPlanEndDate().isBefore(LocalDate.now()))) {
            throw new RuntimeException("User's membership is not active. Status: " + user.getMembershipStatus() + ".");
        }
        // Also ensure current plan is actually active if it exists, as membershipStatus might just be a string.
        // We can refine this validation to check currentPlanEndDate as well,
        // although deriveAndSetUserStatus in UserService should keep membershipStatus accurate.
        // For robustness, let's explicitly check plan end date too if membershipStatus is "Expired" or "Inactive".
        if ("Expired".equalsIgnoreCase(user.getMembershipStatus())) {
            throw new RuntimeException("User's membership has expired. Please renew the plan.");
        }
        if ("Inactive".equalsIgnoreCase(user.getMembershipStatus())) {
            throw new RuntimeException("User's membership is inactive. Please assign a plan.");
        }


        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Optional<Attendance> existingAttendance = attendanceRepository.findByUserUserIdAndAttendanceDate(userId, today);
        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();
            if (attendance.getCheckOutTime() == null) {
                // User has checked in but not checked out, so this is a CHECK-OUT action
                if (now.isBefore(attendance.getCheckInTime())) {
                    throw new RuntimeException("Check-out time cannot be before check-in time.");
                }

                Duration durationSinceCheckIn = Duration.between(attendance.getCheckInTime(), now);
                if (durationSinceCheckIn.toMinutes() < 10) { // cite: 227, 228
                    throw new RuntimeException("Check-out not allowed. User must stay at least 10 minutes (current duration: " + durationSinceCheckIn.toMinutes() + " minutes)."); // cite: 227
                }

                attendance.setCheckOutTime(now); // cite: 229
                Duration totalDuration = Duration.between(attendance.getCheckInTime(), now);
                attendance.setTimeSpentMinutes(totalDuration.toMinutes());

                return convertToDto(attendanceRepository.save(attendance));
            } else {
                // MODIFIED ERROR MESSAGE: User has already checked in AND checked out today
                throw new RuntimeException("User has already checked in and checked out today at " + attendance.getCheckOutTime().toLocalTime() + "."); // cite: 230
            }
        } else {
            // No attendance record for today, so this is a CHECK-IN action
            Attendance newAttendance = new Attendance(); // cite: 231
            newAttendance.setUser(user); // cite: 231
            newAttendance.setCheckInTime(now); // cite: 231
            newAttendance.setAttendanceDate(today); // cite: 231
            return convertToDto(attendanceRepository.save(newAttendance));
        }
    }

    public List<Attendance> getAttendanceByUserId(String userId) {
         try {
            Integer intUserId = Integer.parseInt(userId); // cite: 232
            User user = userRepository.findById(intUserId) // cite: 232
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)); // cite: 233
            return user.getAttendanceRecords(); // cite: 233
         } catch (NumberFormatException e) {
             throw new RuntimeException("Invalid User ID format: " + userId); // cite: 234
         }
    }

    public Map<LocalDate, Long> getDailyAttendanceCount(LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendanceList = attendanceRepository.findAll(); // cite: 235
        return attendanceList.stream()
                .filter(a -> !a.getCheckInTime().toLocalDate().isBefore(startDate) && // cite: 235
                             !a.getCheckInTime().toLocalDate().isAfter(endDate)) // cite: 235
                .collect(Collectors.groupingBy( // cite: 235
                        a -> a.getCheckInTime().toLocalDate(), // cite: 236
                        Collectors.counting() // cite: 236
                ));
    }

    public Page<AttendanceResponseDTO> getAllAttendanceRecords(Pageable pageable) {
        Page<Attendance> attendancePage = attendanceRepository.findAll(pageable); // cite: 238
        return attendancePage.map(this::convertToDto); // cite: 238
    }

    public void deleteAttendanceRecord(Integer attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) { // cite: 239
            throw new RuntimeException("Attendance record not found with ID: " + attendanceId); // cite: 239
        }
        attendanceRepository.deleteById(attendanceId); // cite: 239
    }

    @Transactional
    public int checkOutAllUsers() {
        LocalDate today = LocalDate.now(); // cite: 240
        List<Attendance> activeAttendances = attendanceRepository.findByCheckOutTimeIsNullAndAttendanceDate(today); // cite: 240

        int checkedOutCount = 0;
        LocalDateTime now = LocalDateTime.now(); // cite: 241
        for (Attendance attendance : activeAttendances) { // cite: 241
            // NEW VALIDATION: Ensure user is still "Active" before checking them out
            if (!"Active".equalsIgnoreCase(attendance.getUser().getMembershipStatus()) &&
                (attendance.getUser().getCurrentPlanEndDate() == null || attendance.getUser().getCurrentPlanEndDate().isBefore(LocalDate.now()))) {
                System.out.println("Skipping check-out for non-active user: " + attendance.getUser().getName() + " (ID: " + attendance.getUser().getUserId() + "). Status: " + attendance.getUser().getMembershipStatus() + ".");
                continue; // Skip inactive/expired users
            }


            if (attendance.getCheckInTime() != null) { // cite: 241
                Duration durationSinceCheckIn = Duration.between(attendance.getCheckInTime(), now); // cite: 242
                if (durationSinceCheckIn.toMinutes() < 10) { // cite: 242
                    System.out.println("Skipping check-out for user " + attendance.getUser().getUserId() + // cite: 242
                                       " (less than 10 minutes stay: " + durationSinceCheckIn.toMinutes() + " min)."); // cite: 243
                    continue; // cite: 243
                }
            } else {
                System.out.println("Skipping check-out for user " + attendance.getUser().getUserId() + " (missing check-in time)."); // cite: 244
                continue; // cite: 244
            }

            if (now.isAfter(attendance.getCheckInTime())) { // cite: 244
                attendance.setCheckOutTime(now); // cite: 245
                Duration duration = Duration.between(attendance.getCheckInTime(), now); // cite: 245
                attendance.setTimeSpentMinutes(duration.toMinutes()); // cite: 245
                attendanceRepository.save(attendance); // cite: 245
                checkedOutCount++; // cite: 245
            }
        }
        return checkedOutCount; // cite: 246
    }
}