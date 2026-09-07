package com.craftix.hostile_humans.mixin;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps scoped GameTest runs from registering every test class in the mod. */
@Mixin(ForgeGameTestHooks.class)
public abstract class ForgeGameTestFilterMix {
    @Redirect(method = "registerGametests", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gametest/framework/GameTestRegistry;register(Ljava/lang/reflect/Method;Ljava/util/Set;)V"), remap = false)
    private static void filterScopedClass(java.lang.reflect.Method method, Set<String> namespaces) {
        String filter = System.getProperty("hostile_humans.gameTestFilter", "").trim();
        if (filter.isEmpty() || isSelected(method.getDeclaringClass().getSimpleName(), filter)) {
            GameTestRegistry.register(method, namespaces);
        }
    }

    private static boolean isSelected(String simpleName, String filter) {
        Set<String> requested = Arrays.stream(filter.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        return requested.contains(simpleName.toLowerCase(Locale.ROOT));
    }
}
