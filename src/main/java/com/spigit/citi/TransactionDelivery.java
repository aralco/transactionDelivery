package com.spigit.citi;

import com.spigit.citi.service.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TransactionDelivery {
    public static void main(String args[])  {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");
        Service service = (Service) context.getBean("transactionDeliveryService");
        service.execute();
    }
}
