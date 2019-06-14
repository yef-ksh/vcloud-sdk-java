package com.bytedance.open.http;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class HttpClientFactory {

    private static ConnectionKeepAliveStrategy connectionKeepAliveStrategy;

    public static ExecutorService executorService = newSingleThreadExecutor();


    public static HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {  //retry handler
        public boolean retryRequest(IOException exception,
                                    int executionCount, HttpContext context) {
            if (executionCount >= 5) {
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                return true;
            }
            if (exception instanceof InterruptedIOException) {
                return false;
            }
            if (exception instanceof UnknownHostException) {
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext
                    .adapt(context);
            HttpRequest request = clientContext.getRequest();

            if (!(request instanceof HttpEntityEnclosingRequest)) {
                return true;
            }
            return false;
        }
    };

    public static HttpClient create(ClientConfiguration configuration) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        int maxCon = configuration.getMaxConnections();
        int maxConPerRoute = configuration.getMaxConPerRoute();
        connectionManager.setMaxTotal(maxCon);
        connectionManager.setDefaultMaxPerRoute(maxConPerRoute);

        ConnectionKeepAliveStrategy strategy;
        if (connectionKeepAliveStrategy != null) {
            strategy = connectionKeepAliveStrategy;
        } else {
            strategy = getConnectionKeepAliveStrategy();
        }

        HttpClient httpClient;
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(strategy)
                .setRetryHandler(httpRequestRetryHandler)
                .setDefaultRequestConfig(RequestConfig.custom().setStaleConnectionCheckEnabled(true).build())
                .build();

        executorService.submit(new IdleConnectionMonitorThread(connectionManager));
        return httpClient;
    }

    public static ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator
                        (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase
                            ("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                return 60 * 1000;//如果没有约定，则默认定义时长为60s
            }
        };
    }

    public static void setConnectionKeepAliveStrategy(ConnectionKeepAliveStrategy connectionKeepAliveStrategy) {
        HttpClientFactory.connectionKeepAliveStrategy = connectionKeepAliveStrategy;
    }
}