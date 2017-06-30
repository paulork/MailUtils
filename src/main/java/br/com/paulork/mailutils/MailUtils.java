package br.com.paulork.mailutils;

import java.util.Properties;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

public class MailUtils {

    public static boolean verificaConexao(String server, int port, final String email, final String pass, AuthType tipoAuth) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", true);

            if (tipoAuth.equals(AuthType.TLS)) {
                props.put("mail.smtp.starttls.enable", true);
            } else if (tipoAuth.equals(AuthType.SSL)) {
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
            }

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, pass);
                }
            });

            Transport transport = session.getTransport("smtp");
            transport.connect(server, port, email, pass);
            transport.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

}
