package com.gym.gymmanagementsystem.service;

import com.gym.gymmanagementsystem.dto.PaymentDTO;
import com.gym.gymmanagementsystem.dto.PaymentResponseDTO;
import com.gym.gymmanagementsystem.model.MembershipPlan;
import com.gym.gymmanagementsystem.model.Payment;
import com.gym.gymmanagementsystem.model.User;
import com.gym.gymmanagementsystem.repository.MembershipPlanRepository;
import com.gym.gymmanagementsystem.repository.PaymentRepository;
import com.gym.gymmanagementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    @Autowired
    private UserService userService;

    // Helper to convert Payment entity to PaymentResponseDTO
    private PaymentResponseDTO convertToDto(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setUserId(payment.getUser().getUserId());
        dto.setUserName(payment.getUser().getName());
        dto.setAmount(payment.getAmount());
        dto.setDueAmount(payment.getDueAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentMethodDetail(payment.getPaymentMethodDetail());
        dto.setMembershipPlanId(payment.getMembershipPlanId());
        dto.setTransactionId(payment.getTransactionId());
        dto.setNotes(payment.getNotes());

        if (payment.getMembershipPlanId() != null) {
            membershipPlanRepository.findById(payment.getMembershipPlanId()).ifPresent(plan -> {
                dto.setMembershipPlanName(plan.getPlanName());
            });
        }
        return dto;
    }

    @Transactional
    public PaymentResponseDTO addPayment(PaymentDTO paymentDTO) {
        User user = userRepository.findById(paymentDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + paymentDTO.getUserId()));

        Double planPrice = 0.0;
        MembershipPlan selectedPlan = null;

        // If a membership plan is selected, fetch its price for validation and due calculation
        if (paymentDTO.getMembershipPlanId() != null) {
            selectedPlan = membershipPlanRepository.findById(paymentDTO.getMembershipPlanId())
                    .orElseThrow(() -> new RuntimeException("Membership Plan not found with ID: " + paymentDTO.getMembershipPlanId()));
            planPrice = selectedPlan.getPrice();
        }

        Double dueAmountCalculated = 0.0; // This is the due for the *new* payment being added
        if (selectedPlan != null) { // Only calculate due against a plan price
            if (paymentDTO.getAmount() < planPrice) {
                dueAmountCalculated = planPrice - paymentDTO.getAmount();
            } else if (paymentDTO.getAmount() > planPrice) {
                // Allow overpayment, but set due to 0 and maybe log it
                System.out.println("Warning: User " + user.getUserId() + " paid more than plan price. Overpayment: " + (paymentDTO.getAmount() - planPrice));
                dueAmountCalculated = 0.0; // No due if overpaid
            }
        }
        // If no plan selected, dueAmountCalculated remains 0.0 (as it's not a plan payment)

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(paymentDTO.getAmount());
        payment.setDueAmount(dueAmountCalculated); // Set the calculated due amount for THIS new payment
        payment.setPaymentDate(paymentDTO.getPaymentDate());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setPaymentMethodDetail(paymentDTO.getPaymentMethodDetail());
        payment.setMembershipPlanId(paymentDTO.getMembershipPlanId());
        payment.setTransactionId(paymentDTO.getTransactionId());
        payment.setNotes(paymentDTO.getNotes());

        // Process membership plan assignment/renewal if a plan is specified
        if (selectedPlan != null) {
            if (user.getCurrentPlanId() != null && user.getCurrentPlanEndDate() != null && user.getCurrentPlanEndDate().isAfter(LocalDate.now())) {
                user.setCurrentPlanEndDate(user.getCurrentPlanEndDate().plusMonths(selectedPlan.getDurationMonths()));
                System.out.println("User " + user.getUserId() + " plan extended. New end date: " + user.getCurrentPlanEndDate());
            } else {
                user.setCurrentPlanId(selectedPlan.getPlanId());
                user.setCurrentPlanStartDate(paymentDTO.getPaymentDate());
                user.setCurrentPlanEndDate(paymentDTO.getPaymentDate().plusMonths(selectedPlan.getDurationMonths()));
                System.out.println("User " + user.getUserId() + " new plan assigned. End date: " + user.getCurrentPlanEndDate());
            }
            
            userService.deriveAndSetUserStatus(user);
            userRepository.save(user); // Save the updated user (with new plan dates and derived status)
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Handle updating original payment's due amount if this is a "Pay Due" transaction
        if (paymentDTO.getOriginalPaymentId() != null) {
            Payment originalPayment = paymentRepository.findById(paymentDTO.getOriginalPaymentId())
                    .orElseThrow(() -> new RuntimeException("Original payment not found with ID: " + paymentDTO.getOriginalPaymentId()));
            
            // Calculate new due for the original payment
            double newOriginalDue = originalPayment.getDueAmount() - paymentDTO.getAmount();
            originalPayment.setDueAmount(Math.max(0.0, newOriginalDue)); // Ensure due doesn't go negative
            paymentRepository.save(originalPayment); // Save the updated original payment
            System.out.println("Updated original payment " + originalPayment.getPaymentId() + " due to: " + originalPayment.getDueAmount());
        }

        return convertToDto(savedPayment);
    }

    public Page<PaymentResponseDTO> getAllPayments(Pageable pageable) {
        Page<Payment> paymentsPage = paymentRepository.findAll(pageable);
        return paymentsPage.map(this::convertToDto);
    }

    public PaymentResponseDTO getPaymentById(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
        return convertToDto(payment);
    }

    public List<PaymentResponseDTO> getPaymentsByUserId(Integer userId) {
        List<Payment> payments = paymentRepository.findByUserUserId(userId);
        if (payments.isEmpty()) {
            throw new RuntimeException("No payments found for user ID: " + userId);
        }
        return payments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePayment(Integer paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new RuntimeException("Payment record not found with ID: " + paymentId);
        }
        paymentRepository.deleteById(paymentId);
    }

    public Map<String, Object> getPaymentAnalytics(LocalDate startDate, LocalDate endDate) {
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);

        double totalAmountCollected = payments.stream().mapToDouble(Payment::getAmount).sum();
        long totalPaymentsCount = payments.size();
        double totalDueAmount = payments.stream().mapToDouble(Payment::getDueAmount).sum();

        double totalExpectedAmount = totalAmountCollected + totalDueAmount; // Sum of all plan prices for payments in range

        Map<String, Double> amountByMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod, Collectors.summingDouble(Payment::getAmount)));

        double cashCollected = amountByMethod.getOrDefault("Cash", 0.0);
        double cardCollected = amountByMethod.getOrDefault("Card", 0.0);
        double onlineCollected = amountByMethod.getOrDefault("Online", 0.0);

        Map<String, Long> countByMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod, Collectors.counting()));

        Map<String, Double> amountByPlan = payments.stream()
                .filter(p -> p.getMembershipPlanId() != null)
                .collect(Collectors.groupingBy(p -> membershipPlanRepository.findById(p.getMembershipPlanId())
                        .map(MembershipPlan::getPlanName).orElse("Unknown Plan"),
                        Collectors.summingDouble(Payment::getAmount)));

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalAmountCollected", totalAmountCollected);
        analytics.put("totalPaymentsCount", totalPaymentsCount);
        analytics.put("totalDueAmount", totalDueAmount);
        analytics.put("totalExpectedAmount", totalExpectedAmount);
        analytics.put("cashCollected", cashCollected);
        analytics.put("cardCollected", cardCollected);
        analytics.put("onlineCollected", onlineCollected);
        analytics.put("amountByPaymentMethod", amountByMethod);
        analytics.put("countByPaymentMethod", countByMethod);
        analytics.put("amountByMembershipPlan", amountByPlan);

        return analytics;
    }
}