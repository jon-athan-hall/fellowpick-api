package com.fellowpick.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;

// Builds the branded HTML + plain-text emails sent by the auth flows. A lone bare link in a
// plain-text body with a generic subject reads like phishing to spam filters, so every message
// carries both parts, names the product, and explains why the recipient is getting it.
final class AuthEmail {

    private AuthEmail() {
    }

    // Assembles a multipart/alternative message with a call-to-action link.
    static MimeMessage build(JavaMailSender mailSender, String from, String to, String subject,
                             String recipientName, String intro, String buttonLabel, String link,
                             String footer) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(
                    plainText(recipientName, intro, link, footer),
                    html(recipientName, intro, buttonLabel, link, footer));
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build email: " + subject, e);
        }
        return message;
    }

    private static String plainText(String name, String intro, String link, String footer) {
        return "Hi " + name + ",\n\n"
                + intro + "\n\n"
                + link + "\n\n"
                + footer + "\n\n"
                + "— Fellowpick\n";
    }

    // Inline styles only: most mail clients strip <style> blocks. The name is user-supplied,
    // so everything interpolated here is escaped.
    private static String html(String name, String intro, String buttonLabel, String link, String footer) {
        String safeLink = HtmlUtils.htmlEscape(link);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:24px;background:#f4f4f5;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#18181b;">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:8px;padding:32px;">
                    <p style="margin:0 0 24px;font-size:20px;font-weight:700;">Fellowpick</p>
                    <p style="margin:0 0 16px;font-size:16px;line-height:1.5;">Hi %s,</p>
                    <p style="margin:0 0 24px;font-size:16px;line-height:1.5;">%s</p>
                    <p style="margin:0 0 24px;">
                      <a href="%s" style="display:inline-block;background:#1c7ed6;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 20px;border-radius:6px;">%s</a>
                    </p>
                    <p style="margin:0 0 8px;font-size:14px;line-height:1.5;color:#52525b;">If the button doesn't work, paste this link into your browser:</p>
                    <p style="margin:0 0 24px;font-size:14px;line-height:1.5;word-break:break-all;"><a href="%s" style="color:#1c7ed6;">%s</a></p>
                    <p style="margin:0;font-size:14px;line-height:1.5;color:#52525b;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(name),
                HtmlUtils.htmlEscape(intro),
                safeLink,
                HtmlUtils.htmlEscape(buttonLabel),
                safeLink,
                safeLink,
                HtmlUtils.htmlEscape(footer));
    }
}
