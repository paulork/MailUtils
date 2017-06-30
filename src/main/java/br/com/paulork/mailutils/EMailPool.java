package br.com.paulork.mailutils;

import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class EMailPool extends Thread {

    private static EMailPool instance = null;
    private static final ConcurrentLinkedQueue<EMail> POOL = new ConcurrentLinkedQueue<>();
    private static boolean ENVIAR;

    /**
     * DEFAULT value is TRUE
     */
    private EMailPool() {
        this(true);
    }
    
    private EMailPool(Boolean enviar) {
        ENVIAR = (enviar == null ? false : enviar);
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);
    }

    public void addEMail(EMail email) {
        POOL.offer(email);
        synchronized (POOL) {
            POOL.notify();
        }
    }

    public static synchronized EMailPool getInstance() {
        if (instance == null) {
            instance = new EMailPool();
            instance.start();
        }
        return instance;
    }
    
    public static synchronized EMailPool getInstance(Boolean enviar) {
        if (instance == null) {
            instance = new EMailPool(enviar);
            instance.start();
        }
        return instance;
    }

    @Override
    public void run() {
        setName("EMailPool");
        try {
            while (!this.isInterrupted()) {
                while (!POOL.isEmpty()) {
                    EMail mail = POOL.poll();
                    if (ENVIAR) {
                        try {
                            sendMail(mail);
                        } catch (MessagingException ex) {
                            System.err.println("ERRO AO ENVIAR EMAIL: ");
                            ex.printStackTrace();
                        }
                    }
                }
                synchronized (POOL) {
                    POOL.wait();
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void sendMail(EMail mail) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp"); //define protocolo de envio como SMTP
        props.put("mail.smtp.host", mail.getServer()); //server SMTP
        props.put("mail.smtp.port", mail.getPort()); //porta
        props.put("mail.smtp.auth", mail.getAuthType() != null); //ativa autenticacao
        props.put("mail.smtp.user", mail.getUser()); //usuario ou seja, a conta que esta enviando o email

        if (mail.getAuthType() != null) {
            if (mail.getAuthType().equals(AuthType.TLS)) {
                props.put("mail.smtp.starttls.enable", true);
            } else if (mail.getAuthType().equals(AuthType.SSL)) {
                props.put("mail.smtp.socketFactory.port", mail.getPort());
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
        }

        props.put("mail.smtp.dsn.notify", (mail.isConfirmEntrega() ? "SUCCESS," : "") + "FAILURE");

        //Cria um autenticador que sera usado a seguir
        Session session = null;
        if (mail.getAuthType() != null) {
            session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mail.getUser(), mail.getPass());
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        Message msg = new MimeMessage(session);
        Multipart corpoPrincipal = new MimeMultipart("related");
        if (mail.isConfirmLeitura()) {
            msg.setHeader("Disposition-Notification-To", mail.getRemetente());
            msg.setHeader("Return-Receipt-To", mail.getRemetente());
            msg.setHeader("X-Confirm-Reading-To", mail.getRemetente());
        }

        //Setando o destinatário
        Address[] listAddress = new Address[mail.getPara().size()];
        for (int q = 0; q < mail.getPara().size(); q++) {
            listAddress[q] = new InternetAddress(mail.getPara().get(q));
        }
        msg.setRecipients(Message.RecipientType.TO, listAddress); //Setando a origem do email
        msg.setFrom(new InternetAddress(mail.getRemetente())); //Setando a origem do email
        msg.setSubject(mail.getAssunto()); //Setando o assunto

        // Monta o conteudo que Vai o email ....
        MimeBodyPart conteudoEmail = new MimeBodyPart();
        conteudoEmail.setContent(mail.getMensagem(), mail.getContentType());
        // conteudoEmail.setText(mailBody);
        conteudoEmail.addHeader("Content-Type", mail.getContentType());
        // adiciona o conteudo do e-mail no corpo do e-mail
        corpoPrincipal.addBodyPart(conteudoEmail);

        for (String nomeAnexo : mail.getAnexos().keySet()) {
            // Monta o anexo do e-mail
            MimeBodyPart attach = new MimeBodyPart();
            attach.setDisposition(Part.ATTACHMENT);
            attach.setDataHandler(new DataHandler(new ByteArrayDataSource(mail.getAnexos().get(nomeAnexo), "application/x-any")));
            attach.setFileName(nomeAnexo);
            // adiciona o anexo no corpo do e-mail
            corpoPrincipal.addBodyPart(attach);
        }

        msg.setContent(corpoPrincipal, mail.getContentType());
        msg.saveChanges();

        Transport transp = session.getTransport("smtp");
        transp.connect(mail.getServer(), mail.getPort(), mail.getUser(), mail.getPass());
        transp.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
        transp.close();
    }
    
    public static int getPoolSize() {
        return POOL.size();
    }

}
