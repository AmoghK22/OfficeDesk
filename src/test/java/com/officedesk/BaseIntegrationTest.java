package com.officedesk;

import com.officedesk.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;

    @Autowired protected JwtUtil jwtUtil;

    @Autowired protected com.officedesk.repository.UserRepository userRepo;

    @Autowired protected com.officedesk.repository.DepartmentRepository deptRepo;

    @Autowired protected com.officedesk.repository.TicketRepository ticketRepo;

    @Autowired protected com.officedesk.repository.SlaConfigRepository slaConfigRepo;

    @MockBean protected JavaMailSender javaMailSender;

    protected String tokenFor(com.officedesk.entity.User user) {
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }
}
