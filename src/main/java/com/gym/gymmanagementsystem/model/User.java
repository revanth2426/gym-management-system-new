// src/main/java/com/gym/gymmanagementsystem/model/User.java - COMPLETE FILE
package com.gym.gymmanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "users")
@Data
@NamedEntityGraph( // MODIFIED: Remove 'attendanceRecords' from top level
    name = "User.withAssignmentsAndAttendance",
    attributeNodes = {
        @NamedAttributeNode(value = "planAssignments", subgraph = "planAssignmentsGraph") // Keep this
        // REMOVED THIS LINE: @NamedAttributeNode("attendanceRecords") // <--- THIS LINE MUST BE REMOVED
    },
    subgraphs = {
        @NamedSubgraph(name = "planAssignmentsGraph", attributeNodes = @NamedAttributeNode("membershipPlan"))
        // If attendanceRecords had a subgraph, remove it here too
    }
)
public class User {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name", nullable = false)
    private String name;

    private Integer age;

    private String gender;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "membership_status")
    private String membershipStatus;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Keep LAZY here
    private java.util.List<PlanAssignment> planAssignments;

    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Keep LAZY here
    private java.util.List<Attendance> attendanceRecords;
}