package com.example.sociomap2;

import android.os.AsyncTask;
import android.util.Log;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender extends AsyncTask<Void, Void, Boolean> {
    private String[] recipientEmails;
    private String subject;
    private String messageBody;

    // Constructor accepting multiple recipients
    public EmailSender(String[] recipientEmails, String subject, String messageBody) {
        this.recipientEmails = recipientEmails;
        this.subject = subject;
        this.messageBody = messageBody;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        final String senderEmail = "tobik.brnak@gmail.com";  // Replace with your sender email
        final String senderPassword = "vozy wzhi ygka xeua"; // Replace with your app password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));

            // Add all recipients
            for (String recipient : recipientEmails) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            Log.e("EmailSender", "Error sending email: " + e.getMessage());
            return false;
        }
    }
}
