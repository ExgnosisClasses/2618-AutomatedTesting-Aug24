package com.example.banking.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class ConfigController {

    @GetMapping("/system")
    public Map<String, Object> systemInfo() {
        Map<String, Object> info = new HashMap<>();
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            info.put(key.toString(), props.get(key));
        }
        info.put("env", System.getenv());
        return info;
    }

    @GetMapping("/threads")
    public String[] activeThreads() {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        String[] names = new String[threads.length];
        for (int i = 0; i < threads.length; i++) {
            names[i] = threads[i] != null ? threads[i].getName() : null;
        }
        return names;
    }

    @PostMapping("/shutdown")
    public String shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            System.exit(0);
        }).start();
        return "shutting down";
    }

    @GetMapping("/exec")
    public String exec(@RequestParam String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(cmd);
        return "started pid=" + p.pid();
    }
}
