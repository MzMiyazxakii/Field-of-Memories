package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
public final class OldBookRecipeUnlock {
    private static final ResourceLocation OLD_BOOK_RECIPE =
            new ResourceLocation(FieldOfMemories.MOD_ID, "old_book");

    private OldBookRecipeUnlock() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.server.getRecipeManager().byKey(OLD_BOOK_RECIPE)
                .ifPresent(recipe -> player.awardRecipes(List.of(recipe)));
    }
}
