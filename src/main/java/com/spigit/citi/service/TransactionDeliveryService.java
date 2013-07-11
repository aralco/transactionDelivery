package com.spigit.citi.service;

import com.spigit.citi.dao.TransactionQueueDao;
import com.spigit.citi.model.TransactionQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

public class TransactionDeliveryService extends Service {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDeliveryService.class);

    @Autowired
    private TransactionQueueDao transactionQueueDAO;
    private EmailService emailService;

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void execute() {
        logger.info("*************************TransactionDelivery*************************");
        List<TransactionQueue> transactionQueues = getTransactionQueues();
        if(transactionQueues!=null && !transactionQueues.isEmpty()) {
            transactionQueues = assemblyAndSendSMTPMessages(transactionQueues);
            updateTransactionQueueStatus(transactionQueues);
        } else   {
              logger.warn("There are no transactions in queue.");
        }
    }

    private List<TransactionQueue> getTransactionQueues()   {
        logger.info("Loading READY/FAILURE transactions.");
        return transactionQueueDAO.getNotSuccessTransactionQueues();
    }

    private List<TransactionQueue> assemblyAndSendSMTPMessages(List<TransactionQueue> transactionQueues)   {
        logger.info("Sending transactions through SMTP messages.");
        for(TransactionQueue transactionQueue : transactionQueues)  {
            transactionQueue = emailService.assemblyAndSendSMTPMessage(transactionQueue);
        }
        return transactionQueues;
    }

    private void updateTransactionQueueStatus(List<TransactionQueue> transactionQueues)   {
        logger.info("Update transaction status.");
        transactionQueueDAO.updateTransactionQueues(transactionQueues);
    }
}
