package net.noah.cbconverter.networking;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.colony.buildings.modules.BuildingResourcesModule;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.noah.cbconverter.NbtMaterialChecklist;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// From Client to Server (Client "Requests" the Resources Message from the Server)
public class RequestResourcesMessage {
    private final CompoundTag resourceScrollNbtTag;

    // Constructor for creating the message from the client (client side?)
    public RequestResourcesMessage(CompoundTag resourceScrollNbtTag) {
        this.resourceScrollNbtTag = resourceScrollNbtTag;
    }

    // Constructor for decoding the message from the network (server side?)
    public RequestResourcesMessage(FriendlyByteBuf friendlyByteBuf) {
        this.resourceScrollNbtTag = friendlyByteBuf.readNbt();
    }

    // Encode the message for network transmission
    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeNbt(this.resourceScrollNbtTag);
    }

    public Map<ItemStack, Integer> getDataFromResourceScrollServer(CompoundTag ResourceScrollNbtTag, ServerPlayer player) {

        // Get Colony Information
        int colonyID = ResourceScrollNbtTag.getInt("colony");
        BlockPos builderPos = ResourceScrollNbtTag.contains("builder") ? BlockPosUtil.read(ResourceScrollNbtTag, "builder") : null;
        //IColonyView colonyView = IColonyManager.getInstance().getColonyView(colonyID, Minecraft.getInstance().level.dimension());
        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyID, player.serverLevel().dimension());

        if (colony == null) {
            System.err.println("Colony is null");
            return null;
        }
        // Works
        //LOGGER.debug("Colony ID: {}", colonyID);
        //LOGGER.debug("Builder Pos: {}", builderPos);

//        if (colonyView == null) {
//            System.err.println("Colony View is null");
//            return null;
//        }
        else {
            System.out.println("Found colony: " + colony.getName());
            System.out.println("Colony is not null. Cool.");

//            IBuildingView buildingView = colonyView.getBuilding(builderPos);
            IBuilding building = colony.getBuildingManager().getBuilding(builderPos);

            if ( !(building instanceof BuildingBuilder) ) {
                System.err.println("Building is not a BuildingBuilder");
                return null;
            }
            else {
                System.out.println("Yay. The builder is apparently building. Nice!");
                BuildingBuilder buildingBuilder = (BuildingBuilder) building;

                BuildingResourcesModule resourcesModule = buildingBuilder.getModulesByType(BuildingResourcesModule.class).get(0);

                if (resourcesModule == null) {
                    System.err.println("Resources Module View is null");
                    return null;
                }
                else {
                    System.out.println("Resources Module View is not null. Very nice.");

                    Map<String, BuildingBuilderResource> requiredResources = resourcesModule.getNeededResources();
                    //System.out.println("Required Resources: {}", requiredResources);

                    Map<ItemStack, Integer> extractedResources = new HashMap<>(); // This is the returned Map

                    for (Map.Entry<String, BuildingBuilderResource> entry : requiredResources.entrySet()) {
                        String resourceName = entry.getKey();
                        BuildingBuilderResource resource = entry.getValue();

                        try {
                            int resourceAmount = resource.getAmount();
                            ItemStack resourceStack = resource.getItemStack();
                            CompoundTag resourceTag = resourceStack.getTag();
//                          System.out.println("Resource Name: {}", resourceName);
//                          System.out.println("Resource Amount: {}", resourceAmount);
//                          System.out.println("Resource Stack: {}", resourceStack);
//                          System.out.println("Resource Tag: {}", resourceTag);
                            extractedResources.put(resourceStack, resourceAmount);
                        } catch (Exception e) {
                            System.err.println("Error processing resource: " + resourceName + "--- Error Message: ---\n" + e.getMessage());
                        }
                    }
                    return extractedResources; // This is a Map<ItemStack, Integer>
                }
            }
        }
    }

    public ItemStack convertToClipboardDataServer(Map<ItemStack, Integer> ExtractedResources) {
        if (ExtractedResources == null || ExtractedResources.isEmpty()) {
            System.err.println("Extracted Resources is null or empty");
            return null;
        }
        else {
            System.out.println("Starting conversion of "+ ExtractedResources.size() +" unique items to Create Clipboard Data");

            // Checklist for materials creation, gets filled in the for loop below
            NbtMaterialChecklist checklist = new NbtMaterialChecklist();

            int processed_items = 0;
            int failed_items = 0;
            for (Map.Entry<ItemStack, Integer> entry: ExtractedResources.entrySet()) {
                ItemStack originalStack = entry.getKey();
                Integer originalAmount = entry.getValue();

                // Check
                if (originalStack == null || originalStack.isEmpty() || originalAmount == null) {
                    System.err.println("Error processing item: "+ originalStack.getDisplayName().getString() +" - Amount or Item is Null or Empty. Skipping this item.");
                    continue;
                }

                // If the item is from Domum Ornamentum, it needs additional nbt data stored in the clipboard
                boolean isDomumOrnamentum = originalStack.getItem().toString().contains("domum_ornamentum"); // TODO: is there a better way to check this?

                try {
                    System.out.println("Processing item: "+ originalStack.getDisplayName().getString() +" - Amount: " + originalAmount.toString());
                    ItemStack convStackWithAmount = originalStack.copy();
                    convStackWithAmount.setCount(originalAmount);
                    System.out.println("Created ItemStack with Quantity: "+ convStackWithAmount.getDisplayName().getString() +" x"+ convStackWithAmount.getCount());

                    ItemRequirement requirement;

                    // Detect if NBT data is available, very important for Domum Ornamentum Items
                    boolean hasNBT = convStackWithAmount.hasTag();
                    if (hasNBT) {
                        System.out.println("Has NBT: "+ convStackWithAmount.getTag().toString());
                        convStackWithAmount.setTag(originalStack.getTag());

                        // Create StrictNbtStackRequirement for the item with NBT data
                        ItemRequirement.StackRequirement strictRequirement = new ItemRequirement.StrictNbtStackRequirement(convStackWithAmount, ItemRequirement.ItemUseType.CONSUME);
                        requirement = new ItemRequirement(List.of(strictRequirement));
                        System.out.println("Created ItemRequirement with StrictNbtStackRequirement for item "+ convStackWithAmount.getDisplayName().getString());
                    }
                    else {
                        System.out.println("Does not have NBT");

                        // Create a 'regular' ItemRequirement for the item since it has no important NBT data
                        requirement = new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, convStackWithAmount);
                        System.out.println("Created ItemRequirement with ItemRequirement for item "+ convStackWithAmount.getDisplayName().getString());
                    }

                    // Add the requirement to the MaterialChecklist
                    checklist.require(requirement);
                    System.out.println("Added requirement for item "+ convStackWithAmount.getDisplayName().getString() +" to MaterialChecklist");

                    processed_items++;
                } catch (Exception e) {
                    System.err.println("Error processing item: "+ originalStack.getDisplayName().getString() +" - "+ e.getMessage());
                    failed_items++;
                }
            }
            // End of for loop, now the MaterialChecklist is filled with (hopefully) all neccessary items
            System.out.println("Finished processing "+ processed_items +" items, "+ failed_items +" failed.\nMaterialChecklist is created.");

            // Now, create the final Clipboard with the items
            try {
                ItemStack finalClipboard = checklist.createWrittenClipboardNbt();
                System.out.println("Created Clipboard");

                // Check if it actually has items in it
                if (finalClipboard.isEmpty()) {
                    System.err.println("Final Clipboard is empty, shit.");
                    return null;
                }

                System.out.println("Finished conversion to Create Clipboard with "+ processed_items +" unique items. Yippie!");
                return finalClipboard;
            } catch (Exception e) {
                System.err.println("Error creating Clipboard: "+ e.getMessage());
                return null;
            }
        }
    }

    // Handle the message on the server side (this is where the actual work is done)
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) { return; }

            // Server side logic here
            System.out.println("Server received Resource Scroll conversion request from " + sender.getName().getString() + " with NBT: " + this.resourceScrollNbtTag.toString() + ".");

            Map<ItemStack, Integer> extractedResources = this.getDataFromResourceScrollServer(this.resourceScrollNbtTag, sender);
            if (extractedResources == null) {
                System.err.println("Error extracting resources from Resource Scroll.");
                return;
            }
            else {
                System.out.println("Extracted " + extractedResources.size() + " unique items from Resource Scroll.");
            }

            // Convert to Clipboard and get ItemStack
            ItemStack finalClipboard = this.convertToClipboardDataServer(extractedResources);
            System.out.println("Final Clipboard NBT Data: "+ finalClipboard.getTag().toString());

            if (finalClipboard == null) {
                System.err.println("Error converting to Clipboard.");
                ModMessages.sendToClient(new ResponseResourcesMessage(null, false), sender);
                return;
            }

            System.out.println("Server: Converted to Clipboard.");

            // Replace Converted Clipboard in Player Offhand
            try {
                sender.setItemInHand(InteractionHand.OFF_HAND, finalClipboard);
                sender.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F, 1.0F);
                System.out.println("Server: Updated Clipboard in Offhand.");
                sender.displayClientMessage(Component.literal("Successfully copied into Clipboard"), true);
            } catch (Exception e) {
                System.err.println("Error updating Clipboard in Offhand: "+ e.getMessage());
                ModMessages.sendToClient(new ResponseResourcesMessage(null, false), sender);
                //e.printStackTrace();
            }
            ModMessages.sendToClient(new ResponseResourcesMessage(finalClipboard, true), sender);
        });
        context.setPacketHandled(true);
    }
}
