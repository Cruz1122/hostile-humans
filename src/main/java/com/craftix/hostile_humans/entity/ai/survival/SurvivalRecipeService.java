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
import net.minecraft.world.item.crafting.ShapedRecipe;

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
        for (CraftingRecipe recipe : candidates(human, desired)) {
            ItemStack advertised = recipe.getResultItem(human.level().registryAccess());
            if (advertised.isEmpty()) continue;
            for (int size : tableAvailable ? new int[]{2, 3} : new int[]{2}) {
                Optional<ItemStack> crafted = tryCraft(human, recipe, size, true);
                if (crafted.isPresent()) return crafted;
            }
        }
        return Optional.empty();
    }

    public static boolean canCraft(Human human, Predicate<ItemStack> desired, boolean tableAvailable) {
        if (human.level().isClientSide || human.getData() == null) return false;
        for (CraftingRecipe recipe : candidates(human, desired)) {
            for (int size : tableAvailable ? new int[]{2, 3} : new int[]{2}) {
                if (tryCraft(human, recipe, size, false).isPresent()) return true;
            }
        }
        return false;
    }

    private static List<CraftingRecipe> candidates(Human human, Predicate<ItemStack> desired) {
        return human.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> desired.test(recipe.getResultItem(human.level().registryAccess())))
                .sorted(Comparator.comparingInt((CraftingRecipe recipe) ->
                        GearUpgradePolicy.score(recipe.getResultItem(human.level().registryAccess()))).reversed())
                .toList();
    }

    private static Optional<ItemStack> tryCraft(Human human, CraftingRecipe recipe, int size, boolean consume) {
        List<Ingredient> ingredients = recipe.getIngredients();
        long nonEmpty = ingredients.stream().filter(ingredient -> !ingredient.isEmpty()).count();
        if (nonEmpty > size * size) return Optional.empty();
        CraftingContainer grid = new TransientCraftingContainer(new DummyMenu(), size, size);
        List<Reserved> reserved = reserveIngredients(human, ingredients);
        if (reserved.isEmpty() && nonEmpty > 0) return Optional.empty();
        int inputIndex = 0;
        for (Ingredient ingredient : ingredients) {
            Reserved match = reserved.stream().filter(entry -> !entry.used && ingredient.test(entry.stack)).findFirst().orElse(null);
            if (!ingredient.isEmpty()) {
                if (match == null) return Optional.empty();
                match.used = true;
                int x;
                int y;
                if (recipe instanceof ShapedRecipe shaped) {
                    x = inputIndex % shaped.getWidth();
                    y = inputIndex / shaped.getWidth();
                } else {
                    x = inputIndex % size;
                    y = inputIndex / size;
                }
                if (x >= size || y >= size) return Optional.empty();
                grid.setItem(y * size + x, match.stack.copyWithCount(1));
            }
            inputIndex++;
        }
        if (!recipe.matches(grid, human.level())) return Optional.empty();
        ItemStack output = recipe.assemble(grid, human.level().registryAccess());
        if (output.isEmpty()) return Optional.empty();
        NonNullList<ItemStack> remainders = recipe.getRemainingItems(grid);
        List<ItemStack> produced = new ArrayList<>();
        produced.add(output);
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty()) produced.add(remainder);
        }
        if (!SurvivalInventory.canStore(human, produced)) return Optional.empty();
        if (!consume) return Optional.of(output.copy());
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
