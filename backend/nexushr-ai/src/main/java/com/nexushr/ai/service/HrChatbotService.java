package com.nexushr.ai.service;

import com.nexushr.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrChatbotService {

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:mock-key}")
    private String apiKey;

    public ChatResponse chat(String message) {
        if ("mock-key".equalsIgnoreCase(apiKey) || apiKey.trim().isEmpty()) {
            log.info("OpenAI API key is mock or empty. Using local QA knowledge base fallback.");
            return ChatResponse.builder()
                    .response(getLocalResponse(message))
                    .source("LOCAL_KNOWLEDGE_BASE")
                    .build();
        }

        try {
            log.info("Sending chat request to OpenAI");
            String systemContext = "You are NexusHR Copilot, an AI HR Assistant. You help employees and HR managers with policy questions, leave rules, payroll structures, performance reviews, and general HR queries.\n"
                    + "Refer to the following standard policy rules if applicable:\n"
                    + "- Casual Leaves: 12 days/year.\n"
                    + "- Sick Leaves: 12 days/year.\n"
                    + "- Earned Leaves: 15 days/year, max carry forward is 30 days.\n"
                    + "- Maternity/Paternity: 180 days.\n"
                    + "- Loss of Pay is unpaid.\n"
                    + "- Payroll cycle is monthly. Tax cuts: standard EPF, ESI, TDS calculations.\n"
                    + "Please answer this user query concisely:\n\n";
            String response = chatModel.call(systemContext + message);
            return ChatResponse.builder()
                    .response(response)
                    .source("LLM")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to contact OpenAI API, falling back to local QA: {}", e.getMessage());
            return ChatResponse.builder()
                    .response(getLocalResponse(message))
                    .source("LOCAL_KNOWLEDGE_BASE (FALLBACK)")
                    .build();
        }
    }

    private String getLocalResponse(String message) {
        String msg = message.toLowerCase();

        if (msg.contains("leave") || msg.contains("vacation") || msg.contains("holiday")) {
            return "### NexusHR Leave Policy Guidelines\n\n"
                    + "NexusHR provides the following leaves:\n"
                    + "1. **Casual Leave (CL)**: 12 days per calendar year. Designed for personal/urgent matters.\n"
                    + "2. **Sick Leave (SL)**: 12 days per calendar year. Medical certification is required for > 3 consecutive days.\n"
                    + "3. **Earned Leave (EL)**: 15 days per year. Accrued monthly. You can carry forward up to 30 days.\n"
                    + "4. **Maternity Leave**: 180 days of paid leave.\n"
                    + "5. **Loss of Pay (LOP)**: Unpaid leave when paid leave balance is exhausted.\n\n"
                    + "To apply, navigate to the **Leave & Attendance** dashboard.";
        }

        if (msg.contains("payroll") || msg.contains("salary") || msg.contains("payslip") || msg.contains("epf") || msg.contains("tax")) {
            return "### NexusHR Payroll & Tax Information\n\n"
                    + "- **Payroll Cycle**: Payroll runs monthly on the last working day.\n"
                    + "- **Tax Deductions (India)**:\n"
                    + "  - **EPF (Provident Fund)**: 12% of basic salary from employee share, matched by employer.\n"
                    + "  - **ESI (State Insurance)**: Applicable to employees with gross salary <= Rs. 21,000/month.\n"
                    + "  - **TDS (Income Tax)**: Deducted monthly according to the selected Tax Regime (Old/New).\n"
                    + "- **Payslips**: Downloadable as secure PDFs from the **Payroll Portal**.";
        }

        if (msg.contains("performance") || msg.contains("goal") || msg.contains("okr") || msg.contains("review")) {
            return "### Performance & Goals Portal\n\n"
                    + "- **Review Frequency**: Quarterly SMART goals & Annual OKR evaluations.\n"
                    + "- **Cycle Status**: Check active and upcoming review cycles under **Performance Management**.\n"
                    + "- **Ratings Normalization**: HR Admin conducts a bell-curve mapping based on: 10% A+, 20% A, 50% B, 15% C, 5% D.";
        }

        if (msg.contains("attrition") || msg.contains("risk") || msg.contains("skills") || msg.contains("gap")) {
            return "### AI Intelligence Copilot\n\n"
                    + "- **Attrition Model**: Runs daily heuristic scoring checking employee tenure, salary percentile, leave patterns, and latest ratings.\n"
                    + "- **Skill Gap Recommender**: Compares employee skills (JSONB fields) with designation standards and compiles targeted course/certification recommendations.";
        }

        return "Hello! I am **NexusHR Copilot**, your AI HR assistant.\n\n"
                + "I can help you with queries about:\n"
                + "- **Leave Policies** (e.g. 'How many sick leaves do I get?')\n"
                + "- **Payroll & Payslips** (e.g. 'When is the salary cycle?')\n"
                + "- **Performance Cycles** (e.g. 'How are goals tracked?')\n"
                + "- **AI Analytics** (e.g. 'How is the attrition risk computed?')\n\n"
                + "Please ask a question about any of these areas!";
    }
}
