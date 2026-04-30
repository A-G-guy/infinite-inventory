package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RemixIconResourceTest {

    private static final String BASE_PATH =
            "src/main/resources/assets/infiniteinventory/textures/gui/sprites/";

    @Test
    void everyRemixIconShouldHavePngFile() {
        for (RemixIcon icon : RemixIcon.values()) {
            String path = BASE_PATH + icon.location().getPath() + ".png";
            assertTrue(Files.exists(Paths.get(path)),
                    "Missing PNG for icon: " + icon.name() + " at " + path);
        }
    }
}
