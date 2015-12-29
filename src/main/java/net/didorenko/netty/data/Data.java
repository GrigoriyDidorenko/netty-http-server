package net.didorenko.netty.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    public void setBeginTimeAndIp(StatusData statusData, String ip){
        statusData.setSpeed(System.currentTimeMillis());
        statusData.setIp(ip);
    }

    public void processStatusData(StatusData statusData) {
        statusData.setSpeed((statusData.getSentBytes() + statusData.getReceivedBytes())
                * 1000 / (System.currentTimeMillis() - statusData.getSpeed()));
        statusData.setSpeed(new BigDecimal(statusData.getSpeed()).setScale(1, RoundingMode.UP).doubleValue());
        statusData.setDate(LocalDateTime.now());
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
            listOfLastConnections.remove(listOfLastConnections.getFirst());
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
        StringBuffer response = new StringBuffer();
        response.append("<html><head><style>table, th, td {border: 1px solid black; border-collapse: collapse; font-weight: normal;}\n" +
                "th, td {padding: 5px;}\n" +
                "table {margin-left:50px; margin-bottom: 10px;}\n" +
                "body {font-family:Georgia;}\n" +
                "h1 {text-align: center; font-size:25px; font-weight:normal;}\n" +
                "h2 {margin-left:55px; font-size:22px; font-weight:normal;}\n" +
                "</style><h1>Statistic</font></h1></head><body>");
        response.append("<h2>Question #1</h2><table><tr><th>Total connections</th><th>Unique connections</th>");
        response.append("</tr><tr><th>" + queryCounter + "</th><th>" +
                ipCounters.size() +"</th></tr></table>");

        response.append("<h2>Question #2</h2><table><tr><th>IP</th><th>Number of queries</th><th>Timestamp</th></tr><tr>");
        for(Map.Entry<String, IpCounter> urlCounter : ipCounters.entrySet())
            response.append("<tr><th>" + urlCounter.getKey() + "</th>" +
                    "<th>" + urlCounter.getValue().getNumber() + "</th>" +
                    "<th>"+urlCounter.getValue().getLastTimeAccessed()+"</th></tr>");
        response.append("</table>");
        response.append("<h2>Question #3</h2><table><tr><th>Active connections</th>");
        response.append("</tr><tr><th>" + activeCounter + "</th></tr></table>");

        response.append("<h2>Question #4</h2><table><tr><th>URL</th><th>Was accessed times:</th></tr>");
        for(Map.Entry<String, Integer> urlCounter : urlNumberOfRedirects.entrySet())
            response.append("<tr><th>" + urlCounter.getKey() + "</th>" +
                    "<th>" + urlCounter.getValue() + "</th></tr>");
        response.append("</table>");
        response.append("<h2>Question #5</h2><table><tr><th>IP</th><th>URI</th><th>Timestamp</th><th>Sent bytes</th><th>Recieved bytes</th>")
                .append("<th>Speed(bytes/sec)</th></tr>");
        for(StatusData statusData : listOfLastConnections)
        response.append("<tr><th>" + statusData.getIp() + "</th>" +
                "<th>" + statusData.getUri() + "</th>" +
                "<th>" + statusData.getDate()+ "</th>" +
                "<th>" + statusData.getSentBytes() + "</th>" +
                "<th>" + statusData.getReceivedBytes() + "</th>" +
                "<th>" + statusData.getSpeed() + "</th></tr>");
        response.append("</table></body></html>");
        return response.toString();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IpCounter ipCounter = (IpCounter) o;

            if (number != ipCounter.number) return false;
            return !(lastTimeAccessed != null ? !lastTimeAccessed.equals(ipCounter.lastTimeAccessed) : ipCounter.lastTimeAccessed != null);

        }

        @Override
        public int hashCode() {
            int result = lastTimeAccessed != null ? lastTimeAccessed.hashCode() : 0;
            result = 31 * result + number;
            return result;
        }
    }
}
