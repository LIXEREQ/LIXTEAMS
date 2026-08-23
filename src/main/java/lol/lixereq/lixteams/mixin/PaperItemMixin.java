package lol.lixereq.lixteams.mixin;

import lol.lixereq.lixteams.data.datManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class PaperItemMixin {

    private static final String WITHDRAWAL_PREFIX = "[LIXTEAMS-WITHDRAW:";

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void lixteams$onUse(Level level, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack stack = user.getItemInHand(hand);
        if (!stack.is(Items.PAPER)) {
            return;
        }

        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName == null) {
            return;
        }

        String nameText = customName.getString();
        if (!nameText.startsWith(WITHDRAWAL_PREFIX)) {
            return;
        }

        String suffix = nameText.substring(WITHDRAWAL_PREFIX.length()).replace("]", "");
        String[] parts = suffix.split(":");
        if (parts.length < 2) {
            return;
        }

        String currencyName = parts[0];
        long amount;
        try {
            amount = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        String playerUuid = user.getUUID().toString();

        try {
            datManager.get().addPlayerCurrencyBalance(playerUuid, currencyName, amount);
            stack.shrink(1);
            user.sendSystemMessage(Component.literal("Received " + amount + " " + currencyName + " from currency withdrawal paper."));
        } catch (Exception e) {
            user.sendSystemMessage(Component.literal("Failed to process currency withdrawal: " + e.getMessage()));
        }

        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
