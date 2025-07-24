package com.gym.gymmanagementsystem.repository;

import com.gym.gymmanagementsystem.model.PlanAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanAssignmentRepository extends JpaRepository<PlanAssignment, Integer> {

    @Override
    @EntityGraph(value = "PlanAssignment.withUserAndMembershipPlan")
    List<PlanAssignment> findAll();

    @EntityGraph(value = "PlanAssignment.withUserAndMembershipPlan")
    List<PlanAssignment> findByEndDateBetween(LocalDate startDate, LocalDate endDate);

    // NEW: Eagerly fetch user and membershipPlan when finding by User ID
    @EntityGraph(value = "PlanAssignment.withUserAndMembershipPlan")
    List<PlanAssignment> findByUser_UserId(Integer userId); // Note the "User_UserId" naming for nested property
}