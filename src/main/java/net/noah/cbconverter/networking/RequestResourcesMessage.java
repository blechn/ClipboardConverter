package net.noah.cbconverter.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestResourcesMessage {
    public RequestResourcesMessage(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            // do stuff
        });
        context.setPacketHandled(true);
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }
}
