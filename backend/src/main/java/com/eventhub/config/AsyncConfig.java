package com.eventhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Enables @Async so slow work (like sending email) runs on a background thread
@Configuration
@EnableAsync
public class AsyncConfig {
}
