package com.zorroa.archivist.service

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.PasswordResetToken
import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.repository.UserDao
import com.zorroa.common.config.NetworkEnvironment
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStreamReader
import javax.mail.MessagingException

interface EmailService {
    fun sendOnboardEmail(user: User): PasswordResetToken
    fun sendSharedLinkEmail(fromUser: User, toUser: User, link: SharedLink)
    fun sendPasswordResetEmail(user: User): PasswordResetToken

}

@Component
class EmailServiceImpl @Autowired constructor(
        private val userDao: UserDao,
        private val mailSender: JavaMailSender,
        private val networkEnv: NetworkEnvironment
) : EmailService {

    override fun sendSharedLinkEmail(fromUser: User, toUser: User, link: SharedLink) {

        val toName = if (toUser.firstName == null) toUser.username else toUser.firstName
        val fromName = if (fromUser.firstName == null) fromUser.username else fromUser.firstName
        val url = networkEnv.publicUri.toString() + "/search?id=" + link.id

        val text = StringBuilder(1024)
        text.append("Hello ")
        text.append(toName)
        text.append(",\n\n")
        text.append(fromName)
        text.append(" has sent you a link.")
        text.append("\n\n" + url)

        var htmlMsg: String? = null
        try {
            htmlMsg = getTextResourceFile("emails/SharedLink.html")
            htmlMsg = htmlMsg.replace("*|URL|*", url)
            htmlMsg = htmlMsg.replace("*|TO_USER|*", toName)
            htmlMsg = htmlMsg.replace("*|FROM_USER|*", fromName)
        } catch (e: IOException) {
            logger.warn("Failed to open HTML template for sharing links.. Sending text only.", e)
        }

        try {
            sendHTMLEmail(toUser, fromName + " has shared a link with you.", text.toString(), htmlMsg)
        } catch (e: MessagingException) {
            logger.warn("Email for sendPasswordResetEmail not sent, unexpected ", e)
        }

    }

    override fun sendPasswordResetEmail(user: User): PasswordResetToken {
        val token = PasswordResetToken(userDao.setEnablePasswordRecovery(user))

        if (token != null) {
            val name = if (user.firstName == null) user.username else user.firstName
            val url = networkEnv.publicUri.toString() + "/password?token=" + token.toString()

            val text = StringBuilder(1024)
            text.append("Hello ")
            text.append(name)
            text.append(",\n\nClick on the link below to change your Zorroa login credentials.")
            text.append("\n\n" + url)
            text.append("\n\nIf you are not trying to change your Zorroa login credentials, please ignore this email.")

            var htmlMsg: String? = null
            try {
                htmlMsg = getTextResourceFile("emails/PasswordReset.html")
                htmlMsg = htmlMsg.replace("*|RESET_PASSWORD_URL|*", url + "&source=file_server")
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", name)
            } catch (e: IOException) {
                logger.warn("Failed to open HTML template for onboarding. Sending text only.", e)
            }

            try {
                sendHTMLEmail(user, "Zorroa Account Verification", text.toString(), htmlMsg)
                token.isEmailSent = true
            } catch (e: MessagingException) {
                logger.warn("Email for sendPasswordResetEmail not sent, unexpected ", e)
            }

        }
        return token
    }

    override fun sendOnboardEmail(user: User): PasswordResetToken {
        val token = PasswordResetToken(userDao.setEnablePasswordRecovery(user))
        val name = if (user.firstName == null) user.username else user.firstName

        if (token != null) {
            val url = networkEnv.publicUri.toString() + "/onboard?token=" + token.toString()

            val text = StringBuilder(1024)
            text.append("Hello ")
            text.append(name)
            text.append(",\n\nWelcome to Zorroa. Let's get your stuff!")
            text.append(",\n\nClick on the link below to import your assets.")
            text.append("\n\n" + url)

            var htmlMsg: String? = null
            try {
                htmlMsg = getTextResourceFile("emails/Onboarding.html")
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", name)
                htmlMsg = htmlMsg.replace("*|FILE_SERVER_URL|*", url + "&source=file_server")
                htmlMsg = htmlMsg.replace("*|MY_COMPUTER_URL|*", url + "&source=my_computer")
                htmlMsg = htmlMsg.replace("*|CLOUD_SOURCE_URL|*", url + "&source=cloud")
            } catch (e: IOException) {
                logger.warn("Failed to open HTML template for onboarding, Sending text only.", e)
            }

            try {
                sendHTMLEmail(user, "Welcome to Zorroa", text.toString(), htmlMsg)
                token.isEmailSent = true
            } catch (e: MessagingException) {
                logger.warn("Email for sendOnboardEmail not sent, unexpected ", e)
            }

        }
        return token
    }

    @Throws(IOException::class)
    private fun getTextResourceFile(fileName: String): String {
        return CharStreams.toString(InputStreamReader(
                ClassPathResource(fileName).inputStream, Charsets.UTF_8))
    }

    @Throws(MessagingException::class)
    private fun sendHTMLEmail(user: User, subject: String, text: String, htmlMsg: String?) {
        var email = user.email
        if (ArchivistConfiguration.unittest) {
            email = System.getProperty("user.name") + "@zorroa.com"
        }

        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "utf-8")
        helper.setFrom("Zorroa Account Bot <noreply@zorroa.com>")
        helper.setReplyTo("Zorroa Account Bot <noreply@zorroa.com>")
        helper.setTo(email)
        helper.setSubject(subject)
        if (htmlMsg != null) {
            helper.setText(text, htmlMsg)
        } else {
            helper.setText(text)
        }
        mailSender.send(mimeMessage)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)
    }
}
