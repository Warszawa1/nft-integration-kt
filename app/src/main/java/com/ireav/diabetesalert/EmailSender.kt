package com.ireav.diabetesalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.*

class EmailSender(private val context: Context) {

    // Hardcoded values for testing
    private val senderEmail = "iantelovazquez@gmail.com"  // Replace with your Gmail
    private val senderPassword = "hlox tazl hvyo odvs "  // Replace with your Google App Password
    private val recipientEmail = "iantelovazquez@gmail.com"

    suspend fun sendEmail(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("EmailSender", "Starting email send process")
            Log.d("EmailSender", "Using sender email: $senderEmail")
            // Don't log the actual password
            Log.d("EmailSender", "Using recipient: $recipientEmail")

            val props = Properties()
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.socketFactory.port"] = "465"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.port"] = "465"

            Log.d("EmailSender", "Properties set up")

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            session.debug = true // Enable debug mode

            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(senderEmail))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                message.subject = "Emergency Alert"
                message.setText("I need immediate assistance!")

                Log.d("EmailSender", "Message prepared, attempting to send")
                Transport.send(message)
                Log.d("EmailSender", "Email sent successfully")
                true
            } catch (e: MessagingException) {
                Log.e("EmailSender", "MessagingException: ${e.message}")
                e.printStackTrace()
                false
            }
        } catch (e: Exception) {
            Log.e("EmailSender", "General Exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    // Test function to verify values
    fun testValues(): String {
        return """
            Test Results:
            Sender Email: $senderEmail
            Recipient Email: $recipientEmail
            Password length: ${senderPassword.length}
        """.trimIndent()
    }
}
