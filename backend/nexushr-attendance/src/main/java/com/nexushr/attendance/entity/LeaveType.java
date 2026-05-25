package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "default_days", nullable = false)
    private Integer defaultDays;

    @Column(name = "carry_forward", nullable = false)
    private Boolean carryForward;

    @Column(name = "max_carry_days")
    private Integer maxCarryDays;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (carryForward == null) {
            carryForward = false;
        }
        if (isPaid == null) {
            isPaid = true;
        }
        if (defaultDays == null) {
            defaultDays = 0;
        }
    }
}
