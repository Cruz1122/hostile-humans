package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.function.Predicate;

/** Executes only recipes currently loaded by the world's RecipeManager. */
public final class SurvivalRecipeService {
    private SurvivalRecipeService() {}

    public static Optional<ItemStack> craft(Human human, Predicate<ItemStack> desired, boolean tableAvailable) {
        if (human.level().isClientSide || human.getData() == null) return Optional.empty();
        List<CraftingRecipe> candidates = human.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> desired.test(recipe.getResultItem(human.level().registryAccess())))
                .sorted(Comparator.comparingInt((CraftingRecipe recipe) ->
                        GearUpgradePolicy.score(recipe.getResultItem(human.level().registryAccess()))).reversed())
                .toList();
        for (CraftingRecipe recipe : candidates) {
            ItemStack advertised = recipe.getResultItem(human.level().registryAccess());
            if (advertised.isEmpty()) continue;
            for (int size : tableAvailable ? new int[]{2, 3} : new int[]{2}) {
                Optional<ItemStack> crafted = tryCraft(human, recipe, size);
                if (crafted.isPresent()) return crafted;
            }
        }
        return Optional.empty();
    }

    public static boolean hasRecipe(Human human, Predicate<ItemStack> desired) {
        return human.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(recipe -> recipe.getResultItem(human.level().registryAccess()))
                .anyMatch(desired);
    }

    private static Optional<ItemStack> tryCraft(Human human, CraftingRecipe recipe, int size) {
        List<Ingredient> ingredients = recipe.getIngredients();
        long nonEmpty = ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).count();
        if (nonEmpty > size * size) return Optional.empty();
        CraftingContainer grid = new TransientCraftingContainer(new DummyMenu(), size, size);
        List<Reserved> reserved = reserveIngredients(human, ingredients);
        if (reserved.isEmpty() && nonEmpty > 0) return Optional.empty();
        int inputIndex = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                inputIndex++;
                continue;
            }
            Reserved match = reserved.stream().filter(entry -> !entry.used && ingredient.test(entry.stack)).findFirst().orElse(null);
            if (match == null) return Optional.empty();
            match.used = true;
            if (inputIndex >= grid.getContainerSize()) return Optional.empty();
            grid.setItem(inputIndex++, match.stack.copyWithCount(1));
        }
        if (!recipe.matches(grid, human.level())) return Optional.empty();
        ItemStack output = recipe.assemble(grid, human.level().registryAccess());
        if (output.isEmpty() || !SurvivalInventory.canStore(human, output)) return Optional.empty();
        NonNullList<ItemStack> remainders = recipe.getRemainingItems(grid);
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty() && !SurvivalInventory.canStore(human, remainder)) return Optional.empty();
        }
        for (Reserved entry : reserved) if (entry.used) entry.stack.shrink(1);
        ItemStack insertion = output.copy();
        if (SurvivalInventory.insert(human, insertion) != output.getCount()) throw new IllegalStateException("Craft output insertion changed after validation");
        for (ItemStack remainder : remainders) {
            if (remainder.isEmpty()) continue;
            ItemStack insertionRemainder = remainder.copy();
            if (SurvivalInventory.insert(human, insertionRemainder) != remainder.getCount()) {
                throw new IllegalStateException("Craft remainder insertion changed after validation");
            }
        }
        human.markEquipmentDirty();
        human.queueUsefulInventoryEquipment();
        human.queueEquipmentReevaluation();
        SquadNeedsEvaluator.invalidate(human);
        return Optional.of(output.copy());
    }

    private static List<Reserved> reserveIngredients(Human human, List<Ingredient> ingredients) {
        List<Reserved> available = new ArrayList<>();
        for (ItemStack stack : human.getData().getInventoryItems()) {
            for (int count = 0; count < stack.getCount(); count++) available.add(new Reserved(stack));
        }
        List<Reserved> result = new ArrayList<>();
        boolean[] occupied = new boolean[available.size()];
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            int found = -1;
            for (int index = 0; index < available.size(); index++) {
                if (!occupied[index] && ingredient.test(available.get(index).stack)) {
                    found = index;
                    break;
                }
            }
            if (found < 0) return List.of();
            occupied[found] = true;
            result.add(available.get(found));
        }
        return result;
    }

    private static final class Reserved {
        private final ItemStack stack;
        private boolean used;

        private Reserved(ItemStack stack) {
            this.stack = stack;
        }
    }

    private static final class DummyMenu extends AbstractContainerMenu {
        private DummyMenu() { super((MenuType<?>) null, -1); }
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    }
}
