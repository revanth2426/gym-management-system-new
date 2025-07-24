package com.gym.gymmanagementsystem.repository;

import com.gym.gymmanagementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // This EntityGraph now only fetches planAssignments based on User.java definition
    @Override
    @EntityGraph(value = "User.withAssignmentsAndAttendance")
    Page<User> findAll(Pageable pageable);

    // This findById also uses the same EntityGraph
    @EntityGraph(value = "User.withAssignmentsAndAttendance")
    Optional<User> findById(Integer userId);

    // This findByName also uses the same EntityGraph
    @EntityGraph(value = "User.withAssignmentsAndAttendance")
    Optional<User> findByName(String name);

    List<User> findByMembershipStatus(String status);
}