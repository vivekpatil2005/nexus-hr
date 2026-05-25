package com.nexushr.listener;

import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.common.enums.Role;
import com.nexushr.common.event.EmployeeCreatedEvent;
import com.nexushr.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * Event listener that reacts to EmployeeCreatedEvent to automate user credential generation
 * and email notification onboarding.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeCreatedListener {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @EventListener
    @Transactional
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        log.info("Received EmployeeCreatedEvent for employee: {} (email: {})", event.employeeId(), event.email());

        // 1. Generate unique username
        String username = generateUniqueUsername(event.firstName(), event.lastName());

        // 2. Generate temporary password
        String tempPassword = generateTempPassword();

        // 3. Resolve user roles based on employee designation
        Set<Role> roles = resolveRoles(event.designation());

        // 4. Create and save User profile
        User user = User.builder()
                .username(username)
                .email(event.email())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .fullName(event.firstName() + " " + event.lastName())
                .roles(roles)
                .enabled(true)
                .employeeId(event.employeeId())
                .build();

        user = userRepository.save(user);
        log.info("Automatically generated user login profile for employee: id={}, username={}, roles={}", 
                user.getId(), username, roles);

        // 5. Send onboarding email with temporary credentials
        String subject = "Welcome to NexusHR - Your Login Credentials";
        String htmlContent = buildWelcomeEmail(user.getFullName(), username, tempPassword);
        
        notificationService.sendHtmlEmail(user.getId(), user.getEmail(), subject, htmlContent);
    }

    private String generateUniqueUsername(String firstName, String lastName) {
        String base = (firstName.trim() + "." + lastName.trim())
                .toLowerCase()
                .replaceAll("[^a-z.]", "");
        
        if (base.isEmpty()) {
            base = "user";
        }
        
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            suffix++;
            username = base + suffix;
        }
        return username;
    }

    private String generateTempPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$";
        String all = uppercase + lowercase + digits + special;
        
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        
        // Ensure at least one of each class is present
        sb.append(uppercase.charAt(random.nextInt(uppercase.length())));
        sb.append(lowercase.charAt(random.nextInt(lowercase.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        
        // Fill the rest to length 10
        for (int i = 4; i < 10; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        
        // Shuffle characters
        char[] passwordArray = sb.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char a = passwordArray[index];
            passwordArray[index] = passwordArray[i];
            passwordArray[i] = a;
        }
        
        return new String(passwordArray);
    }

    private Set<Role> resolveRoles(String designation) {
        Set<Role> roles = new HashSet<>();
        if (designation == null) {
            roles.add(Role.EMPLOYEE);
            return roles;
        }
        
        String lowerDesig = designation.toLowerCase();
        
        if (lowerDesig.contains("hr") || lowerDesig.contains("people officer") || 
                lowerDesig.contains("recruiter") || lowerDesig.contains("recruitment")) {
            roles.add(Role.HR_MANAGER);
        } else if (lowerDesig.contains("manager") || lowerDesig.contains("director") || 
                lowerDesig.contains("head") || lowerDesig.contains("lead") || 
                lowerDesig.contains("cto") || lowerDesig.contains("cpo") || 
                lowerDesig.contains("ceo") || lowerDesig.contains("president")) {
            roles.add(Role.MANAGER);
        } else {
            roles.add(Role.EMPLOYEE);
        }
        
        return roles;
    }

    private String buildWelcomeEmail(String fullName, String username, String password) {
        return "<html>" +
                "<body style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0c0a09; color: #f5f5f4; padding: 30px; margin: 0;\">" +
                "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #1c1917; border: 1px solid #2e2a24; border-radius: 12px; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.5);\">" +
                "    <div style=\"text-align: center; margin-bottom: 24px;\">" +
                "      <h1 style=\"color: #a78bfa; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;\">NexusHR Onboarding</h1>" +
                "      <p style=\"color: #a8a29e; font-size: 14px; margin: 4px 0 0 0;\">Workforce Intelligence Platform</p>" +
                "    </div>" +
                "    <hr style=\"border: 0; border-top: 1px solid #2e2a24; margin-bottom: 24px;\" />" +
                "    <p style=\"font-size: 16px; line-height: 1.6; color: #e7e5e4;\">Hello <strong>" + fullName + "</strong>,</p>" +
                "    <p style=\"font-size: 15px; line-height: 1.6; color: #d6d3d1;\">Welcome to the team! Your employee profile has been registered in the NexusHR portal. A user login account has been automatically provisioned for you.</p>" +
                "    <div style=\"background-color: #292524; border: 1px dashed #44403c; border-radius: 8px; padding: 20px; margin: 24px 0;\">" +
                "      <p style=\"margin: 0 0 8px 0; font-size: 14px; color: #a8a29e;\">YOUR SECURITY CREDENTIALS:</p>" +
                "      <table style=\"width: 100%; border-collapse: collapse;\">" +
                "        <tr>" +
                "          <td style=\"padding: 6px 0; font-size: 15px; color: #d6d3d1; width: 140px;\"><strong>Username:</strong></td>" +
                "          <td style=\"padding: 6px 0; font-size: 15px; color: #818cf8; font-family: monospace;\">" + username + "</td>" +
                "        </tr>" +
                "        <tr>" +
                "          <td style=\"padding: 6px 0; font-size: 15px; color: #d6d3d1;\"><strong>Temp Password:</strong></td>" +
                "          <td style=\"padding: 6px 0; font-size: 15px; color: #f472b6; font-family: monospace; font-weight: bold;\">" + password + "</td>" +
                "        </tr>" +
                "      </table>" +
                "    </div>" +
                "    <p style=\"font-size: 14px; line-height: 1.5; color: #a8a29e; font-style: italic; margin-bottom: 24px;\">Note: For security reasons, you will be required to change this temporary password upon your first login. You can do this in the Settings section.</p>" +
                "    <div style=\"text-align: center; margin-bottom: 24px;\">" +
                "      <a href=\"http://localhost:5173/login\" style=\"background-color: #7c3aed; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block; box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);\">Login to NexusHR</a>" +
                "    </div>" +
                "    <hr style=\"border: 0; border-top: 1px solid #2e2a24; margin-top: 24px; margin-bottom: 16px;\" />" +
                "    <p style=\"font-size: 12px; text-align: center; color: #78716c; margin: 0;\">This is an automated system email. Please do not reply directly.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}
