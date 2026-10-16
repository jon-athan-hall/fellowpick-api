package com.fellowpick.auth;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.when;

// Helpers for asserting on MimeMessages built against a mocked JavaMailSender.
final class MailTestSupport {

    private MailTestSupport() {
    }

    // A mocked JavaMailSender returns null from createMimeMessage(); hand it a real, session-less message.
    static void stubCreateMimeMessage(JavaMailSender mailSender) {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    // Returns the decoded text of every text part, so assertions don't depend on transfer encoding.
    static String allText(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                sb.append(allText(multipart.getBodyPart(i))).append('\n');
            }
            return sb.toString();
        }
        return "";
    }
}
