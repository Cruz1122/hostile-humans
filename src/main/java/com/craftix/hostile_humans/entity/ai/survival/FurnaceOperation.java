package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;

import java.util.Comparator;
import java.util.Optional;

/** Places input/fuel into a real furnace and retrieves only vanilla-produced output. */
public final class FurnaceOperation {
    public enum Result { WAITING, INSERTED, RETRIEVED, FAILED }

    private FurnaceOperation() {}

    public static Result tick(Human human, BlockPos pos) {
        if (!(human.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) return Result.FAILED;
        ItemStack output = furnace.getItem(2);
        if (!output.isEmpty()) {
            ItemStack transfer = output.copy();
            int inserted = SurvivalInventory.insert(human, transfer);
            if (inserted > 0) {
                output.shrink(inserted);
                furnace.setItem(2, output);
                furnace.setChanged();
                SquadNeedsEvaluator.invalidate(human);
                LocalResourceScanner.invalidate(human);
                human.markEquipmentDirty();
                human.queueEquipmentReevaluation();
                return Result.RETRIEVED;
            }
        }
        if (!furnace.getItem(0).isEmpty()) {
            // An input without remaining fuel can never make progress. Do not
            // leave the survival goal waiting forever beside a dead furnace.
            if (!furnace.getBlockState().getValue(FurnaceBlock.LIT)
                    && furnace.getItem(1).isEmpty()) return Result.FAILED;
            return Result.WAITING;
        }
        Input input = findInput(human).orElse(null);
        if (input == null) return Result.FAILED;
        ItemStack fuel = findFuel(human).orElse(null);
        if (fuel == null) return Result.FAILED;
        int batch = Math.min(8, input.stack.getCount());
        int fuelUnits = Math.min(fuel.getCount(), Math.max(1,
                (batch * 200 + ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) - 1)
                        / ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING)));
        furnace.setItem(0, input.stack.copyWithCount(batch));
        input.stack.shrink(batch);
        furnace.setItem(1, fuel.copyWithCount(fuelUnits));
        fuel.shrink(fuelUnits);
        furnace.setChanged();
        SquadNeedsEvaluator.invalidate(human);
        LocalResourceScanner.invalidate(human);
        return Result.INSERTED;
    }

    public static boolean hasAvailableFuel(Human human) {
        return findFuel(human).isPresent();
    }

    private static Optional<Input> findInput(Human human) {
        if (human.getData() == null) return Optional.empty();
        for (ItemStack stack : human.getData().getInventoryItems()) {
            if (stack.isEmpty()) continue;
            if (!smeltingTarget(stack)) continue;
            SimpleContainer input = new SimpleContainer(stack.copyWithCount(1));
            Optional<? extends AbstractCookingRecipe> recipe =
                    human.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, human.level());
            if (recipe.isPresent()) return Optional.of(new Input(stack, recipe.get()));
        }
        return Optional.empty();
    }

    private static boolean smeltingTarget(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.RAW_IRON) || stack.is(net.minecraft.world.item.Items.RAW_GOLD)
                || stack.is(net.minecraft.world.item.Items.BEEF) || stack.is(net.minecraft.world.item.Items.PORKCHOP)
                || stack.is(net.minecraft.world.item.Items.CHICKEN) || stack.is(net.minecraft.world.item.Items.MUTTON)
                || stack.is(net.minecraft.world.item.Items.RABBIT) || stack.is(net.minecraft.world.item.Items.COD)
                || stack.is(net.minecraft.world.item.Items.SALMON);
    }

    private static Optional<ItemStack> findFuel(Human human) {
        if (human.getData() == null) return Optional.empty();
        return human.getData().getInventoryItems().stream()
                .filter(stack -> !stack.isEmpty() && ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0)
                .filter(stack -> !stack.is(net.minecraft.world.item.Items.CRAFTING_TABLE) && !stack.is(net.minecraft.world.item.Items.FURNACE))
                .min(Comparator.comparingInt(FurnaceOperation::fuelPriority)
                        .thenComparing(Comparator.<ItemStack>comparingInt(stack -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING)).reversed()));
    }

    private static int fuelPriority(ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.COAL)) return 0;
        if (stack.is(net.minecraft.world.item.Items.CHARCOAL)) return 1;
        return 2;
    }

    private record Input(ItemStack stack, AbstractCookingRecipe recipe) {}
}
