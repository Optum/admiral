package com.optum.admiral.model;

import com.optum.admiral.event.StartWaitListener;

import java.net.Socket;

public class StartPortWait implements StartWait {
    final String host;
    final int port;
    final int waitInSeconds;
    final String url;

    public StartPortWait(String host, int port, int waitInSeconds) {
        this.host = host;
        this.port = port;
        this.waitInSeconds = waitInSeconds;
        this.url = host+":"+port;
    }

    @Override
    public int getSeconds() {
        return waitInSeconds;
    }

    @Override
    public boolean waitForIt(StartWaitListener startWaitListener) {
        boolean needed = true;
        int count = 1;
        int total = 10;
        while(needed && (count<=waitInSeconds)) {
            try(Socket check = new Socket(host,port)) {
                // Use check even though we don't need to.
                boolean alive = check.isConnected();
                needed = false;
                startWaitListener.startWaitProgress(count, total, url, "Finished", "Port Alive " + alive);
                return true;
            } catch (Exception e) {
                startWaitListener.startWaitProgress(count, total, url, "Waiting", e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                needed = false;
            }
            count++;
        }
        return false;
    }

}
