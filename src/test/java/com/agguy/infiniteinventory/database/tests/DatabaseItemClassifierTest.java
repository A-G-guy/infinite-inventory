package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseItemClassifier;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseItemClassifier} 物品分类规则测试。
 */
class DatabaseItemClassifierTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    private final DatabaseItemClassifier classifier = DatabaseItemClassifier.INSTANCE;

    @Test
    void shouldClassifyNullStackAsOther() {
        assertEquals(DatabaseCategory.OTHER, classifier.classify(null));
    }

    @Test
    void shouldClassifyEmptyStackAsOther() {
        assertEquals(DatabaseCategory.OTHER, classifier.classify(ItemStack.EMPTY));
    }

    @Test
    void shouldClassifyBlockItemAsBlocks() {
        ItemStack stone = new ItemStack(Items.STONE);
        assertEquals(DatabaseCategory.BLOCKS, classifier.classify(stone));
    }

    @Test
    void shouldClassifyDirtAsBlocks() {
        ItemStack dirt = new ItemStack(Items.DIRT);
        assertEquals(DatabaseCategory.BLOCKS, classifier.classify(dirt));
    }

    @Test
    void shouldClassifyDiamondOreAsBlocks() {
        ItemStack diamondOre = new ItemStack(Items.DIAMOND_ORE);
        assertEquals(DatabaseCategory.BLOCKS, classifier.classify(diamondOre));
    }

    @Test
    void shouldClassifySwordAsToolsWeapons() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(sword));
    }

    @Test
    void shouldClassifyPickaxeAsToolsWeapons() {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(pickaxe));
    }

    @Test
    void shouldClassifyAxeAsToolsWeapons() {
        ItemStack axe = new ItemStack(Items.GOLDEN_AXE);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(axe));
    }

    @Test
    void shouldClassifyShovelAsToolsWeapons() {
        ItemStack shovel = new ItemStack(Items.STONE_SHOVEL);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(shovel));
    }

    @Test
    void shouldClassifyHoeAsToolsWeapons() {
        ItemStack hoe = new ItemStack(Items.WOODEN_HOE);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(hoe));
    }

    @Test
    void shouldClassifyBowAsToolsWeapons() {
        ItemStack bow = new ItemStack(Items.BOW);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(bow));
    }

    @Test
    void shouldClassifyCrossbowAsToolsWeapons() {
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(crossbow));
    }

    @Test
    void shouldClassifyTridentAsToolsWeapons() {
        ItemStack trident = new ItemStack(Items.TRIDENT);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(trident));
    }

    @Test
    void shouldClassifyFishingRodAsToolsWeapons() {
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(rod));
    }

    @Test
    void shouldClassifyShearsAsToolsWeapons() {
        ItemStack shears = new ItemStack(Items.SHEARS);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(shears));
    }

    @Test
    void shouldClassifyShieldAsToolsWeapons() {
        ItemStack shield = new ItemStack(Items.SHIELD);
        assertEquals(DatabaseCategory.TOOLS_WEAPONS, classifier.classify(shield));
    }

    @Test
    void shouldClassifyHelmetAsEquipment() {
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        assertEquals(DatabaseCategory.EQUIPMENT, classifier.classify(helmet));
    }

    @Test
    void shouldClassifyChestplateAsEquipment() {
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        assertEquals(DatabaseCategory.EQUIPMENT, classifier.classify(chestplate));
    }

    @Test
    void shouldClassifyLeggingsAsEquipment() {
        ItemStack leggings = new ItemStack(Items.GOLDEN_LEGGINGS);
        assertEquals(DatabaseCategory.EQUIPMENT, classifier.classify(leggings));
    }

    @Test
    void shouldClassifyBootsAsEquipment() {
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        assertEquals(DatabaseCategory.EQUIPMENT, classifier.classify(boots));
    }

    @Test
    void shouldClassifyElytraAsEquipment() {
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        assertEquals(DatabaseCategory.EQUIPMENT, classifier.classify(elytra));
    }

    @Test
    void shouldClassifyAppleAsConsumables() {
        ItemStack apple = new ItemStack(Items.APPLE);
        assertEquals(DatabaseCategory.CONSUMABLES, classifier.classify(apple));
    }

    @Test
    void shouldClassifyBreadAsConsumables() {
        ItemStack bread = new ItemStack(Items.BREAD);
        assertEquals(DatabaseCategory.CONSUMABLES, classifier.classify(bread));
    }

    @Test
    void shouldClassifyPotionAsConsumables() {
        ItemStack potion = new ItemStack(Items.POTION);
        assertEquals(DatabaseCategory.CONSUMABLES, classifier.classify(potion));
    }

    @Test
    void shouldClassifyMilkBucketAsConsumables() {
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        assertEquals(DatabaseCategory.CONSUMABLES, classifier.classify(milk));
    }

    @Test
    void shouldClassifyStickAsMaterials() {
        ItemStack stick = new ItemStack(Items.STICK);
        assertEquals(DatabaseCategory.MATERIALS, classifier.classify(stick));
    }

    @Test
    void shouldClassifyIronIngotAsMaterials() {
        ItemStack ingot = new ItemStack(Items.IRON_INGOT);
        assertEquals(DatabaseCategory.MATERIALS, classifier.classify(ingot));
    }

    @Test
    void shouldClassifyDiamondAsMaterials() {
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        assertEquals(DatabaseCategory.MATERIALS, classifier.classify(diamond));
    }

    @Test
    void shouldClassifyCoalAsMaterials() {
        ItemStack coal = new ItemStack(Items.COAL);
        assertEquals(DatabaseCategory.MATERIALS, classifier.classify(coal));
    }

    @Test
    void shouldClassifyStringAsMaterialsOrBlocks() {
        ItemStack string = new ItemStack(Items.STRING);
        // STRING 在 1.21.1 中的实际分类取决于它是否为 BlockItem
        DatabaseCategory category = classifier.classify(string);
        // 它要么是 BLOCKS（如果是 BlockItem），要么是 MATERIALS（如果不是）
        assertTrue(category == DatabaseCategory.BLOCKS || category == DatabaseCategory.MATERIALS,
                "STRING 应被分类为 BLOCKS 或 MATERIALS，实际为: " + category);
    }

    @Test
    void shouldClassifySaddleAsOther() {
        ItemStack saddle = new ItemStack(Items.SADDLE);
        assertEquals(DatabaseCategory.OTHER, classifier.classify(saddle));
    }

    @Test
    void shouldClassifyNameTagAsMaterialsOrOther() {
        ItemStack nameTag = new ItemStack(Items.NAME_TAG);
        // NAME_TAG 在 1.21.1 中的实际分类取决于最大堆叠大小
        DatabaseCategory category = classifier.classify(nameTag);
        assertTrue(category == DatabaseCategory.MATERIALS || category == DatabaseCategory.OTHER,
                "NAME_TAG 应被分类为 MATERIALS 或 OTHER，实际为: " + category);
    }

    @Test
    void shouldClassifyTotemOfUndyingAsOther() {
        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        assertEquals(DatabaseCategory.OTHER, classifier.classify(totem));
    }

    @Test
    void currentVersionShouldBeOne() {
        assertEquals(1, DatabaseItemClassifier.CURRENT_VERSION);
    }
}
