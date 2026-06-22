package com.nexushr.listener;

import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.enums.EmployeeStatus;
import com.nexushr.common.event.UserRegisteredEvent;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {} (email: {})", event.username(), event.email());

        // Check if employee already exists for this email
        if (employeeRepository.existsByEmail(event.email())) {
            log.info("Employee already exists for email: {}. Linking employee to user.", event.email());
            Employee employee = employeeRepository.findByEmail(event.email()).get();
            User user = userRepository.findById(event.userId()).get();
            user.setEmployeeId(employee.getId());
            userRepository.save(user);
            return;
        }

        // Create new Employee record
        String[] nameParts = event.fullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        if (lastName.isEmpty()) {
            lastName = "Employee";
        }

        Employee employee = Employee.builder()
                .empCode(generateEmpCode())
                .firstName(firstName)
                .lastName(lastName)
                .email(event.email())
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .designation("Associate")
                .build();

        employee = employeeRepository.save(employee);
        log.info("Created corresponding employee record for user: empCode={}", employee.getEmpCode());

        // Link the user to the new employee
        User user = userRepository.findById(event.userId()).get();
        user.setEmployeeId(employee.getId());
        userRepository.save(user);
    }

    private String generateEmpCode() {
        Integer maxNum = employeeRepository.findMaxEmpCodeNumber();
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return String.format("NEX-%04d", nextNum);
    }
}
