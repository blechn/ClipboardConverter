package net.noah.cbconverter.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// This is what the client receives from the server
public class ResponseResourcesMessage {
    private final ItemStack finalClipboardStack;
    private final boolean success;

    // Constructor logic etc analogous to RequestResourcesMessage, go there for explanations
    public ResponseResourcesMessage(ItemStack finalClipboardStack, boolean success) {
        this.finalClipboardStack = finalClipboardStack;
        this.success = success;
    }

    public ResponseResourcesMessage(FriendlyByteBuf friendlyByteBuf) {
        this.finalClipboardStack = friendlyByteBuf.readItem();
        this.success = friendlyByteBuf.readBoolean();
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeItem(this.finalClipboardStack);
        friendlyByteBuf.writeBoolean(this.success);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
           Player player = Minecraft.getInstance().player;
           if (player == null) { return; }

           if (success) {
               System.out.println("Client: Successfully converted Resource Scroll to Clipboard.");
           } else {
               System.err.println("Client: Failed to convert Resource Scroll to Clipboard.");
           }
        });
        context.setPacketHandled(true);
    }

}
