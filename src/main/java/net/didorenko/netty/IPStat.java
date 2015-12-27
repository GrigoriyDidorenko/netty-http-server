package net.didorenko.netty;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * package: net.didorenko.netty
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 27.12.2015
 */
public class IPStat {

    private LocalDateTime lastTimeRequest;
    private int numberOfQueries;

    public IPStat(LocalDateTime lastTimeRequest, int numberOfQueries) {
        this.lastTimeRequest = lastTimeRequest;
        this.numberOfQueries = numberOfQueries;
    }

    public void addQuery(){
        numberOfQueries++;
        updateLastTimeRequest();
    }

    public void updateLastTimeRequest(){
        lastTimeRequest = LocalDateTime.now();
    }

    public LocalDateTime getLastTimeRequest() {
        return lastTimeRequest;
    }

    public void setLastTimeRequest(LocalDateTime lastTimeRequest) {
        this.lastTimeRequest = lastTimeRequest;
    }

    public int getNumberOfQueries() {
        return numberOfQueries;
    }

    public void setNumberOfQueries(int numberOfQueries) {
        this.numberOfQueries = numberOfQueries;
    }
}
