package com.example.masterproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLauncher {

    @Value("${app.browser.launch}")
    private boolean launchBrowser;

    @Value("${app.browser.url}")
    private String browserUrl;

    @Value("${app.browser.executable}")
    private String firefoxExecutable;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        if (!launchBrowser) {
            return;
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(firefoxExecutable, browserUrl);
            processBuilder.start();
        } catch (Exception ignored) {
        }
    }
}
