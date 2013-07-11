package com.spigit.citi.dao;

import com.spigit.citi.model.TransactionQueryHistory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class TransactionQueryHistoryDaoImpl implements TransactionQueryHistoryDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionQueryHistory> getTransactionQueryHistoryList() {
        Session session = sessionFactory.openSession();
        List<TransactionQueryHistory> transactionQueryHistoryList =  new ArrayList<TransactionQueryHistory>(0);
        transactionQueryHistoryList = (List<TransactionQueryHistory>)session.createQuery("from TransactionQueue").list();
        session.close();
        return transactionQueryHistoryList;

    }

    @Override
    public void saveTransactionQueryHistory(TransactionQueryHistory transactionQueryHistory) {
        Session session = sessionFactory.openSession();
        session.save(transactionQueryHistory);
        session.close();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HashMap<String, String>> getTableProperties() {
        List<HashMap<String, String>> tablePropertiesList = new ArrayList<HashMap<String, String>>(0);
        Session session = sessionFactory.openSession();
        List<?> results = (List<?>)session.createCriteria(TransactionQueryHistory.class)
                .setProjection(Projections.projectionList()
                        .add(Projections.rowCount())
                        .add(Projections.max("timestamp"))
                        .add(Projections.groupProperty("queryType"))
                )
                .list();
        Iterator<?> iterator = results.iterator();
        while (iterator.hasNext())  {
            Object[] objects = (Object []) iterator.next();
            HashMap<String, String> propertiesMap = new HashMap<String, String>(3);
            propertiesMap.put("rows", objects[0].toString());
            propertiesMap.put("timestamp", objects[1].toString());
            propertiesMap.put("queryType", objects[2].toString());
            tablePropertiesList.add(propertiesMap);
        }
        session.close();
        return tablePropertiesList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getNumberOfRows() {
        int numberOfRows = 0;
        Session session = sessionFactory.openSession();
        Criteria criteria = session.createCriteria(TransactionQueryHistory.class)
                .setProjection(Projections.rowCount());

        List<?> result = (List<?>)criteria.list();
        if (!result.isEmpty()) {
            numberOfRows = Integer.valueOf(result.get(0).toString());
        }
        session.close();
        return numberOfRows;
    }
}
