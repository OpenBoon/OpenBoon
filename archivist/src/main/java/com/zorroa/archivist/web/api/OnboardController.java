package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.common.config.NetworkEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by chambers on 5/26/17.
 */
@PreAuthorize("hasAuthority('group::administrator')")
@Controller
public class OnboardController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardController.class);

    @Autowired
    NetworkEnvironment networkEnvironment;

    @Autowired
    UserDao userDao;

    @Autowired
    JavaMailSender mailSender;

    @RequestMapping(value="/api/v1/onboard/_secure", method = RequestMethod.POST)
    public void secure() {
        if (networkEnvironment.getLocation().equals("on-prem")) {
            String pass = HttpUtils.randomString(12);
            userDao.generateHmacKey("admin");
            userDao.setPassword(userDao.get("admin"), pass);
            sendServerAdminPassChangeEmail(pass);
        }
    }

    public void sendServerAdminPassChangeEmail(String pass) {
        StringBuilder text = new StringBuilder(1024);
        text.append("Host: " + networkEnvironment.getPublicUri() + "\n");
        text.append("Region: " + networkEnvironment.getLocation() + "\n");
        text.append("Admin: " + pass + "\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Zorroa Server Bot <noreply@zorroa.com>");
        message.setReplyTo("Zorroa Server Bot <noreply@zorroa.com>");
        message.setTo("support@zorroa.com");
        message.setSubject("Archivist '" + networkEnvironment.getPublicUri() + "' password reset");
        message.setText(text.toString());

        try {
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send initial startup email, pass is '{}' {}", pass, e);
        }
    }
}
