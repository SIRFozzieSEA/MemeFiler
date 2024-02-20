package com.codef.memefiler;

import java.awt.Desktop;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MemeFilerPageLauncher {

    @Value("${MEMEFILER_LAUNCH_PAGE}")
    private String launchUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {

        if (launchUrl != null && launchUrl.length() > 0) {

            System.setProperty("java.awt.headless", "false");
            Desktop desktop = Desktop.getDesktop();

            try {
                desktop.browse(new URI(launchUrl));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}
