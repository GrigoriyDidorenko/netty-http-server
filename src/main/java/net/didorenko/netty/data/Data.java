package net.didorenko.netty.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * package: net.didorenko.netty.data
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 28.12.2015
 */
public class Data {

    private final static Data DATA = new Data();

    public static Data getInstance() {
        return DATA;
    }


    private long queryCounter;
    private long uniqueQueryCounter;
    private long activeCounter;
    private HashMap<String, IpCounter> ipCounters = new HashMap<>();
    private HashMap<String, Integer> urlNumberOfRedirects = new HashMap<>();
    private LinkedList<StatusData> listOfLastConnections = new LinkedList<>();

    public void proccessStatusData(StatusData statusData, String ip) {
        statusData.setSpeed((statusData.getSentBytes() + statusData.getSentBytes())
                * 1000 / (System.currentTimeMillis() - statusData.getSpeed()));
        statusData.setIp(ip);
        addStatusData(statusData);
    }

    public void addQueryCounter() {
        queryCounter++;
    }

    public void addUniqueQueryCounter() {
        uniqueQueryCounter++;
    }

    public void addActiveCounter() {
        activeCounter++;
    }

    public void removeActiveCounter() {
        activeCounter--;
    }

    public void addStatusData(StatusData newElem) {
        if (listOfLastConnections.size() == 16)
            listOfLastConnections.remove(listOfLastConnections.getLast());
        listOfLastConnections.add(newElem);
    }

    public void addIpCounter(String key) {
        if (ipCounters.containsKey(key))
            ipCounters.put(key, new IpCounter(LocalDateTime.now(), ipCounters.get(key).getNumber() + 1));
        else {
            ipCounters.put(key, new IpCounter(LocalDateTime.now(), 1));
            addUniqueQueryCounter();
        }
    }

    public void addUrlNumberOfRedirects(String url) {
        if (urlNumberOfRedirects.containsKey(url))
            urlNumberOfRedirects.put(url, urlNumberOfRedirects.get(url) + 1);
        else
            urlNumberOfRedirects.put(url, 1);
    }

    public String statusResponse() {
      return "<!DOCTYPE html>\n" +
              "<html lang=\"en\">\n" +
              "<head>\n" +
              "    <meta charset=\"UTF-8\">\n" +
              "    <title></title>\n" +
              "</head>\n" +
              "<body>\n" +
              "<p>hello world!</p>\n" +
              "</body>\n" +
              "</html>";
    }

    public long getQueryCounter() {
        return queryCounter;
    }

    public void setQueryCounter(long queryCounter) {
        this.queryCounter = queryCounter;
    }

    public long getUniqueQueryCounter() {
        return uniqueQueryCounter;
    }

    public void setUniqueQueryCounter(long uniqueQueryCounter) {
        this.uniqueQueryCounter = uniqueQueryCounter;
    }

    public long getActiveCounter() {
        return activeCounter;
    }

    public void setActiveCounter(long activeCounter) {
        this.activeCounter = activeCounter;
    }

    public LinkedList<StatusData> getListOfLastConnections() {
        return listOfLastConnections;
    }

    public void setListOfLastConnections(LinkedList<StatusData> listOfLastConnections) {
        this.listOfLastConnections = listOfLastConnections;
    }

    public HashMap<String, IpCounter> getIpCounters() {
        return ipCounters;
    }

    public void setIpCounters(HashMap<String, IpCounter> ipCounters) {
        this.ipCounters = ipCounters;
    }

    public HashMap<String, Integer> getUrlNumberOfRedirects() {
        return urlNumberOfRedirects;
    }

    public void setUrlNumberOfRedirects(HashMap<String, Integer> urlNumberOfRedirects) {
        this.urlNumberOfRedirects = urlNumberOfRedirects;
    }

    class IpCounter {
        LocalDateTime lastTimeAccessed;
        int number;

        public IpCounter(LocalDateTime lastTimeAccessed, int number) {
            this.lastTimeAccessed = lastTimeAccessed;
            this.number = number;
        }

        public LocalDateTime getLastTimeAccessed() {
            return lastTimeAccessed;
        }

        public void setLastTimeAccessed(LocalDateTime lastTimeAccessed) {
            this.lastTimeAccessed = lastTimeAccessed;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}
