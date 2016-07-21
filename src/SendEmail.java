/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jborgerding
 */

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class SendEmail {
    
    private String to;
    private String from;
    private String host;
    private Session session;
    
    public SendEmail(String to, String from, String host, String subject, String text){
        this.to = to;
        this.from = from;
        this.host = host;
        
        // Get system properties
        Properties properties = System.getProperties();
        
        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        
        // Get the default Session object.
        session = Session.getDefaultInstance(properties);
      
    }
    
    public void sendMessage(String subject, String text){
        
        try{
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(subject);

            // Now set the actual message
            message.setText(text);

            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }
        
    }
}
