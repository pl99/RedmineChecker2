package ru.advantum.rmchecker

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j

import javax.mail.Address
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService


/**
 * Created by Kotin on 17.02.2016.
 */
@Slf4j
class Mailer {
    static final String ENCODING = "UTF-8";
    final ConfigObject config
    final ExecutorService threadPool


    ArrayList<RmNotifierStruct> rmNotifierStructArrayList

    Mailer(ConfigObject config/*, ExecutorService threadPool*/) {
        this.config = config
        Map cfg = new HashMap()
        cfg.putAll(config)
        cfg.put("password", "***")
        log.info (cfg.toString())
    }

    void setRmNotifierStructArrayList(ArrayList<RmNotifierStruct> rmNotifierStructArrayList){
        this.rmNotifierStructArrayList=rmNotifierStructArrayList
    }

    void sendMail (){
        File f = new File('mail.template')
        GStringTemplateEngine engine = new groovy.text.GStringTemplateEngine()
        Template template = engine.createTemplate(f)
        println template.toString()

        rmNotifierStructArrayList.findAll {rna-> rna.diff > 0}.each{rna ->
                log.info rna.subject
            assert rna.properties instanceof Map
            Map m = new HashMap()
            m.putAll(rna.properties)
//            m.put("lastChanged", dateFormat.format(rna.lastChanged))



            String content = template.make(rna.properties)
//            """Внимание к задаче ${rna.issueUrl }
//С момента последнего обновления статуса/трекера/приоритета прошло уже ${rna.diff} час.
//Назначена на ${rna.assigned_to_name}
//Последнее обновление ${rna.lastChanged}
//
//Это сообщение отправлено автоматически. Не отвечайте на него
//"""
           Thread.start {sendMail (rna.subject, content, rna.address)}
        }

    }

    void sendMail (String subject, String content, ArrayList<String> address){
        String adr = ""
        address.each {
            adr = adr + it.toString() + ", "
        }
        Address[] addresses = InternetAddress.parse(adr)
        sendMail (subject, content,  addresses)

    }
    void sendMail (String subject, String content, Address[] address){

        String smtpHost = config.smtphost.toString()// "188.93.209.104";

//return
        String from = config.from.toString()
        String login = config.login.toString()
        String password = config.password.toString()
        String smtpPort = config.smtpport.toString()
        sendSimpleMessage(login, password, from, address, content, subject, smtpPort, smtpHost);

    }

    void sendSimpleMessage(String login, String password, String from, Address[] to, String content, String subject, String smtpPort, String smtpHost)
            throws MessagingException, UnsupportedEncodingException {
            Authenticator auth = new SMTPAuthenticator(login, password);

            Properties props = System.getProperties();
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.auth", "true");
            props.put("mail.mime.charset", ENCODING);
            Session session = Session.getDefaultInstance(props, auth);

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, to)
            msg.setSubject(subject);
            msg.setText(content);
            Transport.send(msg);
            log.info "Mail sended!"
    }


static void main(String... args) {

//    new Mailer().SendMail()
    }

    private class SMTPAuthenticator extends Authenticator
    {
        private String user;
        private String password;

        SMTPAuthenticator(String user, String password) {
            this.user = user
            this.password = password
        }

        @Override
        public javax.mail.PasswordAuthentication getPasswordAuthentication()
        {
            return new javax.mail.PasswordAuthentication(this.user, this.password);
        }
    }
}
