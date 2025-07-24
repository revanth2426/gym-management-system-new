package com.gym.gymmanagementsystem.controller;

import com.gym.gymmanagementsystem.dto.AttendanceDTO;
import com.gym.gymmanagementsystem.model.Attendance;
import com.gym.gymmanagementsystem.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.gym.gymmanagementsystem.dto.AttendanceResponseDTO; // NEW IMPORT

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ResponseEntity<AttendanceResponseDTO> checkInUser(@Valid @RequestBody AttendanceDTO attendanceDTO) {
        try {
            Integer userIdInt = Integer.parseInt(attendanceDTO.getUserId());
            AttendanceResponseDTO dto = attendanceService.recordAttendance(userIdInt); // Service now returns DTO
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Attendance>> getAttendanceByUserId(@PathVariable String userId) {
        try {
            List<Attendance> attendanceRecords = attendanceService.getAttendanceByUserId(userId);
            return ResponseEntity.ok(attendanceRecords);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/daily-counts")
    public ResponseEntity<Map<LocalDate, Long>> getDailyAttendanceCounts(
            @RequestParam(name = "startDate") LocalDate startDate,
            @RequestParam(name = "endDate") LocalDate endDate) {
        Map<LocalDate, Long> dailyCounts = attendanceService.getDailyAttendanceCount(startDate, endDate);
        return ResponseEntity.ok(dailyCounts);
    }

    // MODIFIED: Returns List of AttendanceResponseDTO
    @GetMapping("/all")
    public ResponseEntity<Page<AttendanceResponseDTO>> getAllAttendance(
            @RequestParam(defaultValue = "0") int page, // Default to page 0
            @RequestParam(defaultValue = "10") int size) { // Default to 10 records per page
        // Create a Pageable object for sorting by checkInTime descending (most recent first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkInTime").descending());
        Page<AttendanceResponseDTO> attendancePage = attendanceService.getAllAttendanceRecords(pageable);
        return ResponseEntity.ok(attendancePage);
    }
}