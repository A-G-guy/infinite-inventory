package com.agguy.infiniteinventory.resources.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageResourceConsistencyTest {
    @Test
    void zhCnAndEnUsShouldShareTheSameTranslationKeys() throws IOException {
        JsonObject zhCn = this.readLanguageFile("assets/infiniteinventory/lang/zh_cn.json");
        JsonObject enUs = this.readLanguageFile("assets/infiniteinventory/lang/en_us.json");

        Set<String> zhKeys = new TreeSet<>(zhCn.keySet());
        Set<String> enKeys = new TreeSet<>(enUs.keySet());

        assertEquals(zhKeys, enKeys);
        assertTrue(zhKeys.contains("screen.infiniteinventory.common.on"));
        assertTrue(zhKeys.contains("screen.infiniteinventory.common.off"));
        assertTrue(zhKeys.contains("fml.menu.mods.info.description.infiniteinventory"));
    }

    private JsonObject readLanguageFile(String resourcePath) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(inputStream, () -> "未找到资源: " + resourcePath);
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
