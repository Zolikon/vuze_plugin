package com.zolikon.torrentmanager.notifier;

import org.apache.http.HttpHost;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private final String hookUrl;
    private final String userName;

    public NotificationService(String hookUrl, String userName) {
        this.hookUrl = hookUrl;
        this.userName = userName;
    }

    public void sendNotification(String message) {
        try {
            HttpPost httpPost = new HttpPost(hookUrl);
            String payload = String.format("{\"text\": \"%s\",\"username\": \"%s\"}", message, userName);
            httpPost.setEntity(EntityBuilder.create().setText(payload).build());
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpHost httpHost = HttpHost.create("https://hooks.slack.com");
            client.execute(httpHost, httpPost);
        } catch (Exception e) {
            LOG.error("Exception during sending notification: "+e.getMessage());
        }
    }

}
