package com.devops.vault;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "azure.storage.enabled=false",
        "app.storage.local-dir=build/test-vault-storage"
})
class DocumentVaultApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context initializes cleanly with embedded beans
    }
}
