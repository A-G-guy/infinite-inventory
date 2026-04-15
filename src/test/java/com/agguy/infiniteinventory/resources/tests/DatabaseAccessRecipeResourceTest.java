package com.agguy.infiniteinventory.resources.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseAccessRecipeResourceTest {
    @Test
    void databaseAccessRecipeShouldMatchSpecifiedPattern() throws IOException {
        JsonObject recipe = this.readResource("data/infiniteinventory/recipe/database_access_item.json");
        JsonObject key = recipe.getAsJsonObject("key");

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("misc", recipe.get("category").getAsString());
        assertEquals(List.of("DED", "RCR", "DGD"), this.toStringList(recipe.getAsJsonArray("pattern")));
        assertEquals("minecraft:diamond_block", key.getAsJsonObject("D").get("item").getAsString());
        assertEquals("minecraft:ender_pearl", key.getAsJsonObject("E").get("item").getAsString());
        assertEquals("minecraft:redstone_block", key.getAsJsonObject("R").get("item").getAsString());
        assertEquals("minecraft:chest", key.getAsJsonObject("C").get("item").getAsString());
        assertEquals("minecraft:gold_block", key.getAsJsonObject("G").get("item").getAsString());
        assertEquals("infiniteinventory:database_access_item", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void databaseAccessRecipeAdvancementShouldUnlockFromAnyIngredient() throws IOException {
        JsonObject advancement = this.readResource("data/infiniteinventory/advancement/recipes/misc/database_access_item.json");
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        JsonArray requirements = advancement.getAsJsonArray("requirements");

        assertEquals("minecraft:recipes/root", advancement.get("parent").getAsString());
        assertEquals("minecraft:inventory_changed", criteria.getAsJsonObject("has_diamond_block").get("trigger").getAsString());
        assertEquals("minecraft:diamond_block", this.firstCriterionItem(criteria, "has_diamond_block"));
        assertEquals("minecraft:inventory_changed", criteria.getAsJsonObject("has_ender_pearl").get("trigger").getAsString());
        assertEquals("minecraft:ender_pearl", this.firstCriterionItem(criteria, "has_ender_pearl"));
        assertEquals("minecraft:inventory_changed", criteria.getAsJsonObject("has_redstone_block").get("trigger").getAsString());
        assertEquals("minecraft:redstone_block", this.firstCriterionItem(criteria, "has_redstone_block"));
        assertEquals("minecraft:inventory_changed", criteria.getAsJsonObject("has_chest").get("trigger").getAsString());
        assertEquals("minecraft:chest", this.firstCriterionItem(criteria, "has_chest"));
        assertEquals("minecraft:inventory_changed", criteria.getAsJsonObject("has_gold_block").get("trigger").getAsString());
        assertEquals("minecraft:gold_block", this.firstCriterionItem(criteria, "has_gold_block"));
        assertEquals("minecraft:recipe_unlocked", criteria.getAsJsonObject("has_the_recipe").get("trigger").getAsString());
        assertEquals(
                "infiniteinventory:database_access_item",
                criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions").get("recipe").getAsString()
        );
        assertEquals(
                List.of("has_the_recipe", "has_chest", "has_diamond_block", "has_ender_pearl", "has_gold_block", "has_redstone_block"),
                this.toStringList(requirements.get(0).getAsJsonArray())
        );
        assertEquals(
                List.of("infiniteinventory:database_access_item"),
                this.toStringList(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes"))
        );
    }

    private String firstCriterionItem(JsonObject criteria, String criterionName) {
        return criteria.getAsJsonObject(criterionName)
                .getAsJsonObject("conditions")
                .getAsJsonArray("items")
                .get(0)
                .getAsJsonObject()
                .get("items")
                .getAsString();
    }

    private JsonObject readResource(String resourcePath) throws IOException {
        InputStream inputStream = DatabaseAccessRecipeResourceTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(inputStream, () -> "未找到资源: " + resourcePath);
        try (InputStream stream = inputStream; Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private List<String> toStringList(JsonArray jsonArray) {
        List<String> values = new ArrayList<>(jsonArray.size());
        jsonArray.forEach(element -> values.add(element.getAsString()));
        return values;
    }
}
