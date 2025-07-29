package com.gym.gymmanagementsystem.controller;

import com.gym.gymmanagementsystem.dto.ErrorResponseDTO;
import com.gym.gymmanagementsystem.dto.PaymentDTO;
import com.gym.gymmanagementsystem.dto.PaymentResponseDTO;
import com.gym.gymmanagementsystem.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments") // Corrected to /api/payments
@CrossOrigin(origins = {"http://localhost:5173", "http://127.00.1:5173", "https://srfitness-admin.netlify.app"})
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> addPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        try {
            PaymentResponseDTO newPayment = paymentService.addPayment(paymentDTO);
            return new ResponseEntity<>(newPayment, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(new ErrorResponseDTO(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate,desc") String[] sort) {

        Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortBy = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<PaymentResponseDTO> paymentsPage = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(paymentsPage);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPaymentsByUserId(@PathVariable("userId") Integer userId) {
        try {
            List<PaymentResponseDTO> payments = paymentService.getPaymentsByUserId(userId);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new ErrorResponseDTO(e.getMessage(), HttpStatus.NOT_FOUND.value(), System.currentTimeMillis()));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPaymentAnalytics(
            @RequestParam(name = "startDate") LocalDate startDate,
            @RequestParam(name = "endDate") LocalDate endDate) {
        Map<String, Object> analytics = paymentService.getPaymentAnalytics(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable("id") Integer paymentId) {
        try {
            paymentService.deletePayment(paymentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}") // NEW ENDPOINT for updating payments (specifically due amount)
    public ResponseEntity<?> updatePayment(@PathVariable("id") Integer paymentId, @Valid @RequestBody PaymentDTO paymentDTO) {
        try {
            // This method is primarily used for updating the due amount of an existing payment.
            // We can reuse addPayment logic if the DTO includes originalPaymentId,
            // or create a dedicated update method in service.
            // For now, let's assume this is for general updates.
            // If this is specifically for due payment, the logic might need to be in addPayment
            // where originalPaymentId is used.
            // For simplicity, let's just make it a generic update for now.
            // The service method addPayment already handles originalPaymentId logic.
            // So, this PUT endpoint might not be strictly needed if all "due payments" are new POSTs.
            // However, if we need to update *any* field of an existing payment, this is the place.

            // Given the request is to "Pay Due" which creates a new payment and updates an old one,
            // this PUT endpoint might not be directly used by the "Pay Due" button.
            // The "Pay Due" button will POST a *new* payment with originalPaymentId.
            // I'll keep this endpoint as a placeholder for general payment updates if needed later.
            // For the "Pay Due" feature, the logic is handled in PaymentService.addPayment via originalPaymentId.
            
            // To be safe, let's just ensure this endpoint exists if needed for other updates.
            // For "Pay Due" feature, we don't need a separate PUT endpoint in controller.
            // The addPayment method in service already handles updating the original payment.

            // If you want to enable general editing of payment records, you'd implement this.
            // For now, I'll comment out the implementation to avoid confusion with "Pay Due"
            // which creates a new record.
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); // Placeholder
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(new ErrorResponseDTO(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis()));
        }
    }
}