package net.noah.cbconverter;

import com.google.common.collect.Sets;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.CreateLang;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NbtMaterialChecklist extends MaterialChecklist {
    // Private function copied from MaterialChecklist.java
    // Modified for Domum Ornamentum item name handling
    // TODO: Adapt to always try using displayName for compatibility with other mods as well.
    private MutableComponent entry(ItemStack item, int amount, boolean unfinished, boolean forBook) {
        int stacks = amount / 64;
        int remainder = amount % 64;
        MutableComponent tc = Component.empty();

        // Domum Ornamentum Item handling
        boolean isDomumOrnamentum = item.getDescriptionId().toString().contains("domum_ornamentum");

        if (isDomumOrnamentum) {
            Component displayName = item.getDisplayName();
            System.out.println("Using Domum Ornamentum display name: " + displayName.getString());

            tc.append(displayName.copy()
                    .setStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))));
        }
        else {
            tc.append(Component.translatable(item.getDescriptionId())
                    .setStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))));
        }

        if (!unfinished && forBook)
            tc.append(" \u2714");
        if (!unfinished || forBook)
            tc.withStyle(unfinished ? ChatFormatting.BLUE : ChatFormatting.DARK_GREEN);
        return tc.append(Component.literal("\n" + " x" + amount)
                        .withStyle(ChatFormatting.BLACK))
                .append(Component.literal(" | " + stacks + "\u25A4 +" + remainder + (forBook ? "\n" : ""))
                        .withStyle(ChatFormatting.GRAY));
    }

    public Object2IntMap<ItemStack> requiredNbt = new Object2IntArrayMap<>();
    public Object2IntMap<ItemStack> damageRequiredNbt = new Object2IntArrayMap<>();
    public Object2IntMap<ItemStack> gathered = new Object2IntArrayMap<>();

    // I don't get why you should use this when a List of ItemStacks should in theory contain all information, the amount of items but also the NBT data (which is not in the original Object2IntMap)
    // EDIT: I take it back, I found out why. I'll leave this here to understand it again at a later point:
    //
    // The count in ItemStack is a byte, that means when trying to get the amount of the Item from ItemStack, values higher than 128 will overflow into negative (why not use unsigned bytes?)
    // This is also why a new ItemStack was created everytime a new entry is made - it is only for displaying purposes and to prevent the overflow.
    // TODO: Rewrite all this to use ItemStacks purely, because everything else (at least for now seems to be) is unnecessary complication, and using just ItemStacks should be much simpler and elegant

    @Override
    public void require(ItemRequirement requirement) {
        System.out.println(("Custom Require Function called."));
        if (requirement.isEmpty())
            return;
        if (requirement.isInvalid())
            return;

        for (ItemRequirement.StackRequirement stackRequirement : requirement.getRequiredItems()) {
            // This is never used because CONSUME is hardcoded in the rest of the mod
//            if (stack.usage == ItemRequirement.ItemUseType.DAMAGE)
//                putOrIncrementStack(damageRequired, stack.stack);
//            if (stackRequirement.usage == ItemRequirement.ItemUseType.CONSUME)
                putOrIncrementStack(requiredNbt, stackRequirement.stack);
        }
    }

    // This fills the requiredNbt map with the ItemStacks
    private void putOrIncrementStack(Object2IntMap<ItemStack> map, ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item == Items.AIR) return;
        if (map.containsKey(itemStack)) {
            map.put(itemStack, map.getInt(itemStack) + itemStack.getCount()); // adds the amount already in the map to the amount which is required
        }
        else {
            map.put(itemStack, itemStack.getCount());
        }
        System.out.println("Item: " + item.getDescriptionId() + " - Amount: " + map.getInt(itemStack));
    }

    // Is this a necessary overload? Yes it is because of the overflow issues I mentioned above in the comments
    public int getRequiredAmount(ItemStack itemStack) {
        return requiredNbt.getInt(itemStack);
    }

    public ItemStack createWrittenClipboardNbt() {
        ItemStack clipboard = AllBlocks.CLIPBOARD.asStack();
        CompoundTag tag = clipboard.getOrCreateTag();
        int itemsWritten = 0;

        List<List<ClipboardEntry>> pages = new ArrayList<>();
        List<ClipboardEntry> currentPage = new ArrayList<>();

        if (blocksNotLoaded) {
            currentPage.add(new ClipboardEntry(false, CreateLang.translateDirect("materialChecklist.blocksNotLoaded")
                    .withStyle(ChatFormatting.RED)));
        }

        System.out.println(("--------- INSIDE OF CREATION OF THE WRITTEN CLIPBOARD NOW ---------"));

        List<ItemStack> keys = new ArrayList<>(Sets.union(requiredNbt.keySet(), damageRequiredNbt.keySet()));
        Collections.sort(keys, (itemStack1, itemStack2) -> {
            Locale locale = Locale.ENGLISH;
            String name1 = itemStack1.getItem().getDescription()
                    .getString()
                    .toLowerCase(locale);
            String name2 = itemStack2.getItem().getDescription()
                    .getString()
                    .toLowerCase(locale);
            return name1.compareTo(name2);
        });

        System.out.println(("--------- KEYS: ---------"));
        System.out.println(keys);
        System.out.println("Required: \n" + requiredNbt);

        List<ItemStack> completedNbt = new ArrayList<>();
        for (ItemStack itemStack : keys) {
            //int amount = getRequiredAmount(itemStack.getItem());
            int amount = getRequiredAmount(itemStack); // This uses the requiredNbt map, I use it for compatibility I guess but the information should also be already included in ItemStack...
            // Another way would be obviously to just use
            // int amount = itemStack.getCount();
            if (gathered.containsKey(itemStack))
                amount -= gathered.getInt(itemStack);

            if (amount <= 0) {
                completedNbt.add(itemStack);
                continue;
            }

            if (itemsWritten == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
                itemsWritten = 0;
                currentPage.add(new ClipboardEntry(false, Component.literal(">>>")
                        .withStyle(ChatFormatting.DARK_GRAY)));
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }

            System.out.println(("--------- ADDING ITEM: ---------"));
            System.out.println(itemStack);


            itemStack.setCount(1); // This is necessary because if the count is higher than 128, Count overflows into negative and causes issues when trying to order the items from storage etc.
            // Also produces displaying issues in the clipboard
            currentPage.add(new ClipboardEntry(false, entry(itemStack, amount, true, false))
                    .displayItem(itemStack, amount));
            itemsWritten++;

        }

        for (ItemStack itemStack : completedNbt) {
            if (itemsWritten == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
                itemsWritten = 0;
                currentPage.add(new ClipboardEntry(true, Component.literal(">>>")
                        .withStyle(ChatFormatting.DARK_GREEN)));
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }

            itemsWritten++;
            currentPage.add(new ClipboardEntry(true, entry(itemStack, getRequiredAmount(itemStack), false, false))
                    .displayItem(itemStack, 0));
        }

        pages.add(currentPage);
        ClipboardEntry.saveAll(pages, clipboard);
        ClipboardOverrides.switchTo(ClipboardOverrides.ClipboardType.WRITTEN, clipboard);
        clipboard.getOrCreateTagElement("display")
                .putString("Name", Component.Serializer.toJson(CreateLang.translateDirect("materialChecklist")
                        .setStyle(Style.EMPTY.withItalic(Boolean.FALSE))));
        tag.putBoolean("Readonly", true);
        clipboard.setTag(tag);
        return clipboard;
    }
}



































