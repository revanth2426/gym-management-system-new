package com.gym.gymmanagementsystem.repository;

import com.gym.gymmanagementsystem.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
// NEW IMPORTS FOR PAGINATION
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    // MODIFIED: Use Page<Attendance> to return paginated results
    @Override
    @EntityGraph(value = "Attendance.withUser")
    Page<Attendance> findAll(Pageable pageable); // Takes a Pageable object

    // Keep other methods if they exist and are needed elsewhere (e.g., in Dashboard)
    // @EntityGraph(value = "Attendance.withUser") // Example if you needed findByEndDateBetween to be paginated too
    // List<Attendance> findByEndDateBetween(LocalDate startDate, LocalDate endDate);
}