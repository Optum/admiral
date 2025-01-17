package com.optum.admiral.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class URLAdmiralHealthCheck extends HealthCheck {
    final String url;
    final String search;
    final boolean successWhenRedirected;

    public URLAdmiralHealthCheck(String id, String url, String search, boolean successWhenRedirected, long start_period, long timeout, int retries, long interval, long minimum_interval, long rewait_period, long rewait_interval, boolean disabled) {
        super(id, start_period, timeout, retries, interval, minimum_interval, rewait_period, rewait_interval, disabled);
        this.url = url;
        this.search = search;
        this.successWhenRedirected = successWhenRedirected;
    }

    @Override
    public String getTest() {
        return url;
    }

    @Override
    public long execute(DockerModelController dockerModelController, String containerName, int tryCount) {
        dockerModelController.healthCheckProgress(tryCount, retries, id, "Check", getTest());
        final HttpURLConnection connection;
        try {
            URL statusPage = new URL(url);
            connection = (HttpURLConnection)statusPage.openConnection();
            connection.setConnectTimeout((int) timeout);
            connection.setReadTimeout((int) timeout);
            int responseCode = connection.getResponseCode();
            if (responseCode==302) {
                if (successWhenRedirected) {
                    dockerModelController.healthCheckProgress(tryCount, retries, id, "Finished", "Redirect 302 accepted");
                    return 0;
                } else {
                    dockerModelController.healthCheckProgress(tryCount, retries, id, "Not Ready", "Result is 302 Redirect");
                    return -1;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage()==null?"":e.getMessage();
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Error", msg);
            return -2;
        }

        final boolean found;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder builder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            final String homepageHtml = builder.toString();
            found = homepageHtml.contains(search);
        } catch (SocketTimeoutException e) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Not Ready", "Socket Timeout");
            return -1;
        } catch (SocketException e) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Not Ready", "Socket Not Yet Available");
            return -1;
        } catch (FileNotFoundException e) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Not Ready", "URL Not Found");
            return -1;
        } catch (Exception e) {
            String msg = e.getMessage()==null?"":e.getMessage();
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Error", msg);
            return -2;
        }

        if (found) {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Finished", "\"" + search + "\" found");
            return 0;
        } else {
            dockerModelController.healthCheckProgress(tryCount, retries, id, "Missing Search", "\"" + search + "\" not found");
            return -1;
        }
    }

    @Override
    public String getId(String containerName) {
        if (id==null) {
            return containerName + " " + getTest();
        } else {
            return id;
        }
    }

    public String getSearch() {
        return search;
    }

}
