package com.spigit.citi.service;

import com.spigit.citi.common.TransactionStatus;
import com.spigit.citi.model.TransactionQueue;
import com.sun.mail.smtp.SMTPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.NoSuchProviderException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class EmailService  {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String X_ZANTAZ_RECIP = "X-ZANTAZ-RECIP";
    private static final String X_CITIINGSOURCE = "X-CITIINGSOURCE";
    private static final String X_CITIMSGSOURCE = "X-CITIMSGSOURCE";
    private static final String X_CITIMSGTYPE = "X-CITIMSGTYPE";
    private static final String X_CITICONVTYPE = "X-CITICONVTYPE";
    private static final String ENCODING_OPTIONS = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE = "Content-Type";

    private JavaMailSenderImpl javaMailSender;
    private String envelopeFrom;
    private String envelopeTo;

    public void setJavaMailSender(JavaMailSenderImpl javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void setEnvelopeFrom(String envelopeFrom) {
        this.envelopeFrom = envelopeFrom;
    }

    public void setEnvelopeTo(String envelopeTo) {
        this.envelopeTo = envelopeTo;
    }

    public TransactionQueue assemblyAndSendSMTPMessage(TransactionQueue transactionQueue)   {
        logger.info("Assembly SMTP message ");

        // Initialize the JavaMail Session:
        Properties props = System.getProperties();
        props.put("mail.smtp.from", envelopeFrom);
//        props.put("mail.smtp.auth", true);//required when using a gmail account
//        props.put("mail.smtp.starttls.enable", true);//required when using a gmail account
        Session mailsession = Session.getInstance(props, null);

        InputStream myInputStream = null;
        Date date = Calendar.getInstance().getTime();
        try {
            //connect to SMTP host:
            Transport transport;
            transport = mailsession.getTransport("smtp");
            transport.connect(javaMailSender.getHost(), javaMailSender.getPort(), javaMailSender.getUsername(),javaMailSender.getPassword()); // FIXME: need to pull this from the properties file
            /*
            Note: javaMailSender is loading values from email.properties file.
            These values are obtained using "spring-config.xml" and loaded into javaMailSender instance.
            Please let me know if you need improvements or think there is something I am missing.
            CONTACT: ariel.alcocer@gmail.com


            * */

            //Construct the message 
            MimeMessage mimeMessage = new MimeMessage(mailsession);

            //
            // Load original application sender and recipients
            //
            mimeMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(transactionQueue.getMsgTo()));
            mimeMessage.setFrom(new InternetAddress(transactionQueue.getMsgFrom()));

            mimeMessage.setSubject(transactionQueue.getSubject());
            mimeMessage.setSentDate(date);
            myInputStream = transactionQueue.getBody().getBinaryStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(myInputStream));
            String messageBody="";
            String str;
            while ((str = in.readLine()) != null) {
                messageBody += str;
            }
            mimeMessage.setText(messageBody);
            mimeMessage.setHeader(CONTENT_TYPE, ENCODING_OPTIONS);
            mimeMessage.addHeader(X_ZANTAZ_RECIP, transactionQueue.getMsgTo());
            mimeMessage.addHeader(X_CITIINGSOURCE,"FEED2");
            mimeMessage.addHeader(X_CITIMSGSOURCE,"CITIIDEAS");
            mimeMessage.addHeader(X_CITIMSGTYPE,"EMAIL");
            mimeMessage.addHeader(X_CITICONVTYPE,"POST");

            SMTPMessage smtpMessage = new SMTPMessage(mimeMessage);
            //
            // Set the envelope from/to addresses and send the message
            //
            smtpMessage.setEnvelopeFrom(envelopeFrom);
            transport.sendMessage(smtpMessage, InternetAddress.parse(envelopeTo, true));
            transport.close();

            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.SUCCESS.name());
            transactionQueue.setErrorCondition("");
            transactionQueue.setMessageId(getMessageID(mimeMessage.getHeader("Message-ID")));
            logger.info("Message-ID: {}",mimeMessage.getHeader("Message-ID"));
            logger.info("Successful sent SMTP message at {}", date);
        }
        catch (NoSuchProviderException e) {
            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.FAILURE.name());
            transactionQueue.setErrorCondition(e.getMessage());
            logger.warn("Error sending SMTP message {}", e.getMessage());
        }
        catch (MailException e) {
            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.FAILURE.name());
            transactionQueue.setErrorCondition(e.getMessage());
            logger.warn("Error sending SMTP message {}", e.getMessage());
        }
        catch (MessagingException e) {
            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.FAILURE.name());
            transactionQueue.setErrorCondition(e.getMessage());
            logger.warn("Error creating SMTP message {}", e.getMessage());
        } catch (SQLException e)  {
            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.FAILURE.name());
            transactionQueue.setErrorCondition(e.getMessage());
            logger.warn("Error reading Blob body message {}", e.getMessage());
        } catch (IOException e)  {
            //update transactionQueue values
            transactionQueue.setTransmitTime(date);
            transactionQueue.setStatus(TransactionStatus.FAILURE.name());
            transactionQueue.setErrorCondition(e.getMessage());
            logger.warn("Error reading body message {}", e.getMessage());
        } finally {
            if(myInputStream!=null) {
                try {
                    myInputStream.close();
                } catch (IOException e) {
                    logger.error("Error closing Blob inputStream: {}",e.getMessage());
                }
            }
        }
        return transactionQueue;
    }

    private String getMessageID(String[] messageIDs) {
        StringBuilder messageIDBuilder = new StringBuilder();
        if(messageIDs!=null)    {
            for (String s : messageIDs) {
                messageIDBuilder.append(s);
            }
        }
        return messageIDBuilder.toString();
    }
}
