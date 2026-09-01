package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampSavedData;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.ai.camp.CampStorageService;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanCampGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanCampGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "camps", timeoutTicks = 40)
    public static void camp_persists_and_membership_round_trips(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2), "amilcar", UUID.randomUUID());
        Camp camp = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 2)),
                human.getPersonaDefinition().orElseThrow().faction());
        helper.assertTrue(camp != null, "Camp was not physically created");
        human.setCampId(camp.id());
        CompoundTag tag = new CompoundTag();
        human.save(tag);
        helper.assertTrue(tag.hasUUID("CampId"), "CampId was not persisted in entity NBT");
        helper.assertTrue(CampSavedData.get(helper.getLevel()).get(camp.id()) != null, "Camp was not registered");
        helper.assertTrue(helper.getLevel().getBlockState(camp.storagePositions().get(0)).is(net.minecraft.world.level.block.Blocks.CHEST), "Camp chest is missing");
        cleanup(human);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "camps", timeoutTicks = 40)
    public static void storage_deposit_keeps_essential_gear(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2), "aquino", UUID.randomUUID());
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_INGOT, 4));
        Camp camp = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 2)),
                human.getPersonaDefinition().orElseThrow().faction());
        human.setCampId(camp.id());
        helper.assertTrue(CampStorageService.depositExcess(human, camp), "Excess inventory was not deposited");
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(camp.storagePositions().get(0));
        helper.assertTrue(chest != null && contains(chest, Items.IRON_INGOT), "Real chest did not receive excess resource");
        helper.assertTrue(human.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD), "Essential weapon was deposited");
        cleanup(human);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "camps", timeoutTicks = 40)
    public static void storage_withdraw_preserves_remainder(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2), "carola", UUID.randomUUID());
        Camp camp = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 2)),
                human.getPersonaDefinition().orElseThrow().faction());
        human.setCampId(camp.id());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(camp.storagePositions().get(0));
        chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 16));
        helper.assertTrue(CampStorageService.withdrawNeeded(human, camp, stack -> stack.is(Items.COOKED_BEEF), 4), "Food was not withdrawn");
        helper.assertTrue(chest.getItem(0).getCount() == 12, "Withdraw discarded chest remainder");
        helper.assertTrue(count(human, Items.COOKED_BEEF) == 4, "Withdraw transferred the wrong amount");
        cleanup(human);
        helper.succeed();
    }

    private static Human human(GameTestHelper helper, BlockPos pos, String persona, UUID squad) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create camp Human");
        human.moveTo(helper.absolutePos(pos), 0.0F, 0.0F);
        human.setNoAi(true);
        helper.getLevel().addFreshEntity(human);
        if (!human.setPersonaId(persona) || !human.setSquadId(squad)) throw new IllegalStateException("Could not assign camp Human");
        return human;
    }
    private static int count(Human human, net.minecraft.world.item.Item item) { return human.getData().getInventoryItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static boolean contains(net.minecraft.world.Container container, net.minecraft.world.item.Item item) { for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) return true; return false; }
    private static void cleanup(Human... humans) { for (Human human : humans) human.kill(); }
}
