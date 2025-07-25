package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.model.Attendance;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.AttendanceRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public AttendanceResponseDTO recordAttendance(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Optional: Check if user has an active plan before allowing check-in, if desired
        // if (user.getCurrentPlanEndDate() == null || user.getCurrentPlanEndDate().isBefore(LocalDate.now())) {
        //     throw new RuntimeException("User does not have an active plan for check-in.");
        // }

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setCheckInTime(LocalDateTime.now());
        Attendance savedAttendance = attendanceRepository.save(attendance);

        AttendanceResponseDTO dto = new AttendanceResponseDTO();
        dto.setAttendanceId(savedAttendance.getAttendanceId());
        dto.setUserId(savedAttendance.getUser().getUserId());
        dto.setUserName(savedAttendance.getUser().getName());
        dto.setCheckInTime(savedAttendance.getCheckInTime());
        return dto;
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

        return attendancePage.map(attendance -> {
            AttendanceResponseDTO dto = new AttendanceResponseDTO();
            dto.setAttendanceId(attendance.getAttendanceId());
            dto.setUserId(attendance.getUser() != null ? attendance.getUser().getUserId() : null);
            dto.setUserName(attendance.getUser() != null ? attendance.getUser().getName() : "N/A");
            dto.setCheckInTime(attendance.getCheckInTime());
            return dto;
        });
    }
}