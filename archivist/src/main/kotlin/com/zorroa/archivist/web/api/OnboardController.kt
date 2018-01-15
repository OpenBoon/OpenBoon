package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.repository.UserDao
import com.zorroa.common.config.NetworkEnvironment
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@PreAuthorize("hasAuthority('group::administrator')")
@Controller
class OnboardController @Autowired constructor(
        private val networkEnvironment: NetworkEnvironment,
        private val userDao: UserDao,
        private val mailSender: JavaMailSender
){

    @RequestMapping(value = ["/api/v1/onboard/_secure"], method = [RequestMethod.POST])
    fun secure() {
        if (networkEnvironment!!.location == "on-prem") {
            val pass = HttpUtils.randomString(12)
            userDao!!.generateHmacKey("admin")
            userDao!!.setPassword(userDao!!.get("admin"), pass)
            sendServerAdminPassChangeEmail(pass)
        }
    }

    fun sendServerAdminPassChangeEmail(pass: String) {
        val text = StringBuilder(1024)
        text.append("Host: " + networkEnvironment!!.publicUri + "\n")
        text.append("Region: " + networkEnvironment!!.location + "\n")
        text.append("Admin: " + pass + "\n")

        val message = SimpleMailMessage()
        message.from = "Zorroa Server Bot <noreply@zorroa.com>"
        message.replyTo = "Zorroa Server Bot <noreply@zorroa.com>"
        message.setTo("support@zorroa.com")
        message.subject = "Archivist '" + networkEnvironment!!.publicUri + "' password reset"
        message.text = text.toString()

        try {
            mailSender!!.send(message)
        } catch (e: Exception) {
            logger.warn("Failed to send initial startup email, pass is '{}' {}", pass, e)
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(OnboardController::class.java)
    }
}
