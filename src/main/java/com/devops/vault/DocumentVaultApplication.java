package com.devops.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class DocumentVaultApplication {

    private static final Logger log = LoggerFactory.getLogger(DocumentVaultApplication.class);

    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(DocumentVaultApplication.class);
        Environment env = app.run(args).getEnvironment();

        String protocol = "http";
        String serverPort = env.getProperty("server.port", "8080");
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        log.info("""
            ----------------------------------------------------------
            \tApplication: '{}' is running!
            \tAccess URLs:
            \tLocal: \t\t{}://localhost:{}
            \tExternal: \t{}://{}:{}
            \tHealth: \t{}://localhost:{}/actuator/health
            \tMetrics: \t{}://localhost:{}/actuator/prometheus
            \tProfile(s): \t{}
            ----------------------------------------------------------
            """,
                env.getProperty("spring.application.name", "secure-document-vault"),
                protocol, serverPort,
                protocol, hostAddress, serverPort,
                protocol, serverPort,
                protocol, serverPort,
                env.getActiveProfiles().length == 0 ? "default" : String.join(", ", env.getActiveProfiles())
        );
    }
}
