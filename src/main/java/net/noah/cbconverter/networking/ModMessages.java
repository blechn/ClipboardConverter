package net.noah.cbconverter.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.noah.cbconverter.CBConverter;
import net.noah.cbconverter.networking.*;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";

    public static int packetID = 0;

    public static int nextPacketID() {
        return packetID++;
    }

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("cbconverter", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int packetID = 0;

        // 1. Client to server
        INSTANCE.messageBuilder(RequestResourcesMessage.class, nextPacketID(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestResourcesMessage::new)
                .encoder(RequestResourcesMessage::encode)
                .consumerMainThread(RequestResourcesMessage::handle)
                .add();

        // 2. Server to client
        INSTANCE.messageBuilder(ResponseResourcesMessage.class, nextPacketID(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ResponseResourcesMessage::new)
                .encoder(ResponseResourcesMessage::encode)
                .consumerMainThread(ResponseResourcesMessage::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

}
