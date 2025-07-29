package com.gym.gymmanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "due_amount", nullable = false) // NEW FIELD: To store calculated due amount
    private Double dueAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_method_detail")
    private String paymentMethodDetail; // e.g., "PhonePe", "GooglePay", or custom text for "Others"

    @Column(name = "membership_plan_id")
    private Integer membershipPlanId;

    @Column(name = "transaction_id")
    private String transactionId;

    private String notes;
}