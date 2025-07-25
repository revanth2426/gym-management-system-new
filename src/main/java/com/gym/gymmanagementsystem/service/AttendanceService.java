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

    // @Autowired // REMOVED: AttendanceService no longer triggers summary generation directly
    // private AttendanceSummaryService attendanceSummaryService;


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

    @Transactional // Ensures the check-in/out on the temporary table is atomic
    public AttendanceResponseDTO recordOrUpdateAttendance(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

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
                attendance.setCheckOutTime(now);
                Duration duration = Duration.between(attendance.getCheckInTime(), attendance.getCheckInTime()); // Changed to checkInTime
                attendance.setTimeSpentMinutes(duration.toMinutes()); // Set this to 0 if checkOutTime is same as checkInTime

                AttendanceResponseDTO responseDto = convertToDto(attendanceRepository.save(attendance));

                return responseDto;
            } else {
                // User has already checked in AND checked out today
                throw new RuntimeException("User has already checked in and checked out today.");
            }
        } else {
            // No attendance record for today, so this is a CHECK-IN action
            Attendance newAttendance = new Attendance();
            newAttendance.setUser(user);
            newAttendance.setCheckInTime(now);
            newAttendance.setAttendanceDate(today);
            return convertToDto(attendanceRepository.save(newAttendance));
        }
    }

    public List<Attendance> getAttendanceByUserId(String userId) {
         try {
            Integer intUserId = Integer.parseInt(userId);
            User user = userRepository.findById(intUserId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            return user.getAttendanceRecords();
         } catch (NumberFormatException e) {
             throw new RuntimeException("Invalid User ID format: " + userId);
         }
    }

    public Map<LocalDate, Long> getDailyAttendanceCount(LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendanceList = attendanceRepository.findAll();
        return attendanceList.stream()
                .filter(a -> !a.getCheckInTime().toLocalDate().isBefore(startDate) &&
                             !a.getCheckInTime().toLocalDate().isAfter(endDate))
                .collect(Collectors.groupingBy(
                        a -> a.getCheckInTime().toLocalDate(),
                        Collectors.counting()
                ));
    }

    public Page<AttendanceResponseDTO> getAllAttendanceRecords(Pageable pageable) {
        Page<Attendance> attendancePage = attendanceRepository.findAll(pageable);
        return attendancePage.map(this::convertToDto);
    }

    public void deleteAttendanceRecord(Integer attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new RuntimeException("Attendance record not found with ID: " + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }

    // NEW METHOD: Checkout all users who are currently checked in (check_out_time is NULL)
    @Transactional
    public int checkOutAllUsers() {
        LocalDate today = LocalDate.now();
        // Find all records for today where check_out_time is NULL
        List<Attendance> activeAttendances = attendanceRepository.findByCheckOutTimeIsNullAndAttendanceDate(today);

        int checkedOutCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Attendance attendance : activeAttendances) {
            if (attendance.getCheckInTime() != null && now.isAfter(attendance.getCheckInTime())) {
                attendance.setCheckOutTime(now);
                Duration duration = Duration.between(attendance.getCheckInTime(), now);
                attendance.setTimeSpentMinutes(duration.toMinutes());
                attendanceRepository.save(attendance);
                checkedOutCount++;
            }
        }
        // After processing all check-outs, trigger summary generation
        // This ensures the persistent daily_attendance table gets updated for these new check-outs
        // attendanceSummaryService.generateAttendanceSummaries(); // No need to autowire this if you add here
        // If attendanceSummaryService is intended for manual trigger only, this line is not needed here.
        // But if you want the "Checkout All" to also update summaries, uncomment and autowire.
        return checkedOutCount;
    }
}