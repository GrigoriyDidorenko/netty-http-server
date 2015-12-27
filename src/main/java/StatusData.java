import java.time.LocalDateTime;

/**
 * package: PACKAGE_NAME
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 27.12.2015
 */
public class StatusData {

    private String ip;
    private String uri;
    private LocalDateTime date;
    private long sentBytes;
    private long receivedBytes;
    private double speed;

    public StatusData(String ip, String uri, LocalDateTime date, long sentBytes, long receivedBytes, double speed) {
        this.ip = ip;
        this.uri = uri;
        this.date = date;
        this.sentBytes = sentBytes;
        this.receivedBytes = receivedBytes;
        this.speed = speed;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public void setSentBytes(long sentBytes) {
        this.sentBytes = sentBytes;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
