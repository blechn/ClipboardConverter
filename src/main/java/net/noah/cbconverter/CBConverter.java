package net.noah.cbconverter;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.colony.buildings.moduleviews.BuildingResourcesModuleView;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.items.ItemResourceScroll;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockItem;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.noah.cbconverter.networking.ModMessages;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(net.noah.cbconverter.CBConverter.MOD_ID)
public class CBConverter
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "cbconverter";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public CBConverter(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        // what item bro?
        // modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP (CBConverter)");

        event.enqueueWork(() -> {
            ModMessages.register();
        });
    }

    public Map<ItemStack, Integer> getDataFromResourceScroll(ItemStack ResourceScrollStack) {
        // Get NBT Data CompoundTag
        CompoundTag tag = ResourceScrollStack.getOrCreateTag();

        // Get Colony Information
        int colonyID = tag.getInt("colony");
        BlockPos builderPos = tag.contains("builder") ? BlockPosUtil.read(tag, "builder") : null;
        IColonyView colonyView = IColonyManager.getInstance().getColonyView(colonyID, Minecraft.getInstance().level.dimension());

        // Works
        //LOGGER.debug("Colony ID: {}", colonyID);
        //LOGGER.debug("Builder Pos: {}", builderPos);

        if (colonyView == null) {
            LOGGER.error("Colony View is null");
            return null;
        }
        else {
            LOGGER.debug("Colony View is not null. Cool.");
            IBuildingView buildingView = colonyView.getBuilding(builderPos);
            if ( !(buildingView instanceof BuildingBuilder.View) ) {
                LOGGER.error("Building View is not a BuildingBuilder.View");
                return null;
            }
            else {
                LOGGER.debug("Yay. The builder is apparently building. Nice!");
                BuildingBuilder.View buildingBuilderView = (BuildingBuilder.View) buildingView;

                BuildingResourcesModuleView resourcesModuleView = buildingBuilderView.getModuleViewByType(BuildingResourcesModuleView.class);

                if (resourcesModuleView == null) {
                    LOGGER.error("Resources Module View is null");
                    return null;
                }
                else {
                    LOGGER.debug("Resources Module View is not null. Very nice.");

                    Map<String, BuildingBuilderResource> requiredResources = resourcesModuleView.getResources();
                    //LOGGER.debug("Required Resources: {}", requiredResources);

                    Map<ItemStack, Integer> extractedResources = new HashMap<>(); // This is the returned Map

                    for (Map.Entry<String, BuildingBuilderResource> entry : requiredResources.entrySet()) {
                        String resourceName = entry.getKey();
                        BuildingBuilderResource resource = entry.getValue();

                        try {
                            int resourceAmount = resource.getAmount();
                            ItemStack resourceStack = resource.getItemStack();
                            CompoundTag resourceTag = resourceStack.getTag();
//                          LOGGER.debug("Resource Name: {}", resourceName);
//                          LOGGER.debug("Resource Amount: {}", resourceAmount);
//                          LOGGER.debug("Resource Stack: {}", resourceStack);
//                          LOGGER.debug("Resource Tag: {}", resourceTag);
                            extractedResources.put(resourceStack, resourceAmount);
                        } catch (Exception e) {
                            LOGGER.error("Error processing resource: {}", resourceName, e.getMessage());
                        }
                    }
                    return extractedResources; // This is a Map<ItemStack, Integer>
                }
            }
        }
    }

    // TODO: Change return type when it is known!
    public ItemStack convertToClipboardData(Map<ItemStack, Integer> ExtractedResources) {
        if (ExtractedResources == null || ExtractedResources.isEmpty()) {
            LOGGER.error("Extracted Resources is null or empty");
            return null;
        }
        else {
            LOGGER.info("Starting conversion of {} unique items to Create Clipboard Data", ExtractedResources.size());

            // Checklist for materials creation, gets filled in the for loop below
            MaterialChecklist checklist = new MaterialChecklist();

            int processed_items = 0;
            int failed_items = 0;
            for (Map.Entry<ItemStack, Integer> entry: ExtractedResources.entrySet()) {
                ItemStack originalStack = entry.getKey();
                Integer originalAmount = entry.getValue();

                // Check
                if (originalStack == null || originalStack.isEmpty() || originalAmount == null) {
                    LOGGER.error("Error processing item: {} - Amount or Item is Null or Empty", originalStack);
                    return null;
                }
                // If check OK continue here

                try {
                    LOGGER.debug("Processing item: {} - Amount: {}", originalStack, originalAmount);
                    ItemStack convStackWithAmount = originalStack.copy();
                    convStackWithAmount.setCount(originalAmount);
                    LOGGER.debug("Created ItemStack with Quantity: {} x{}", convStackWithAmount.getDisplayName().getString(), convStackWithAmount.getCount());

                    ItemRequirement requirement;

                    // Detect if NBT data is available, very important for Domum Ornamentum Items
                    boolean hasNBT = convStackWithAmount.hasTag();
                    if (hasNBT) {
                        LOGGER.debug("Has NBT: {}", convStackWithAmount.getTag());

                        // Create StrictNbtStackRequirement for the item with NBT data
                        ItemRequirement.StackRequirement strictRequirement = new ItemRequirement.StrictNbtStackRequirement(convStackWithAmount, ItemRequirement.ItemUseType.CONSUME);
                        requirement = new ItemRequirement(List.of(strictRequirement));
                        LOGGER.debug("Created ItemRequirement with StrictNbtStackRequirement for item {}", convStackWithAmount);
                    }
                    else {
                        LOGGER.debug("Does not have NBT");

                        // Create a 'regular' ItemRequirement for the item since it has no important NBT data
                        requirement = new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, convStackWithAmount);
                        LOGGER.debug("Created ItemRequirement with ItemRequirement for item {}", convStackWithAmount);
                    }

                    // Add the requirement to the MaterialChecklist
                    checklist.require(requirement);
                    LOGGER.debug("Added requirement for item {} to MaterialChecklist", convStackWithAmount);

                    processed_items++;
                } catch (Exception e) {
                    LOGGER.error("Error processing item: {} - {}", originalStack, e.getMessage());
                    failed_items++;
                }
            }
            // End of for loop, now the MaterialChecklist is filled with (hopefully) all neccessary items
            LOGGER.debug("Finished processing {} items, {} failed.\nMaterialChecklist is created.", processed_items, failed_items);

            // Now, create the final Clipboard with the items
            try {
                ItemStack finalClipboard = checklist.createWrittenClipboard();
                LOGGER.debug("Created Clipboard");

                // Check if it actually has items in it
                if (finalClipboard.isEmpty()) {
                    LOGGER.error("Final Clipboard is empty, shit.");
                    return null;
                }

                LOGGER.info("Finished conversion to Create Clipboard with {} unique items. Yippie!", processed_items);
                return finalClipboard;
            } catch (Exception e) {
                LOGGER.error("Error creating Clipboard: {}", e.getMessage());
                return null;
            }
        }
    }

    // Event Listener for Player right click (this should be on the client)
    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (event.getLevel().isClientSide) {
            Player player = event.getEntity();
            ItemStack mainHandItem = event.getItemStack();
            ItemStack offHandItem = event.getEntity().getOffhandItem();

            if(mainHandItem.getItem() instanceof ItemResourceScroll &&
            offHandItem.getItem() instanceof ClipboardBlockItem)  {

                // Get data and check
                Map<ItemStack, Integer> dataFromResourceScroll = getDataFromResourceScroll(mainHandItem);
                LOGGER.debug("Resources from ResourceScroll <ItemStack, Integer>:\n{}", dataFromResourceScroll);
                if (dataFromResourceScroll == null) {
                    LOGGER.warn("No resources found in ResourceScroll.");
                    return;
                }

                // Convert the ResourceScroll data to a Create Clipboard - note that we don't get the data back but the actual already converted Clipboard ItemStack
                ItemStack convertedClipboard = convertToClipboardData(dataFromResourceScroll);
                LOGGER.debug("Received a converted Create Clipboard.");

                // Replace the empty Clipboard in offhand with the new converted Clipboard
                player.setItemInHand(InteractionHand.OFF_HAND, convertedClipboard);
                event.getLevel().playSound(player, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
                LOGGER.debug("Replaced empty Clipboard in offhand with the new converted Clipboard. Finished process. Thanks for reading :)");
                player.displayClientMessage(Component.literal("Successfully copied to Clipboard"), true);

            }
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting (CBConverter)");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
