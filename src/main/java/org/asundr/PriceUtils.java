/*
 * Copyright (c) 2025, Arun <offer-at-price-plugin.le03k@dralias.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.asundr;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;

import java.util.Arrays;
import java.util.HashMap;

public class PriceUtils
{
    public static final int TRADEOTHER = InventoryID.TRADEOFFER | 0x8000;
    private static final class ItemID
    {
        public static final int COINS = 995;
        public static final int PLATINUM = 13204;
    }

    private static Client client;
    private static ItemManager itemManager;
    private static Notifier notifier;
    private static ChatMessageManager chatMessageManager;

    // Caches a map of original item IDs to the ID of their noted variant
    private static final HashMap<Integer, Integer> notedIdMap = new HashMap<>();

    public static void initialize(final Client client, final ItemManager itemManager, final Notifier notifier, final ChatMessageManager chatMessageManager)
    {
        PriceUtils.client = client;
        PriceUtils.itemManager = itemManager;
        PriceUtils.notifier = notifier;
        PriceUtils.chatMessageManager = chatMessageManager;
    }

    // Removes html tags from string
    public static String sanitizeWidgetText(final String s)
    {
        return s.replaceAll("<[^>]*>", "").trim();
    }

    // Returns true if passed item id is for coins or platinum chips
    public static boolean isCurrency(final int itemId)
    {
        return itemId == ItemID.COINS || itemId == ItemID.PLATINUM;
    }

    // Returns the total number of an item with the passed ID. Noted items are treated as their un-noted versions.
    public static long getQuantity(final int inventoryId, final int itemId)
    {
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null)
        {
            return 0;
        }
        long total = 0L;
        final long originalID = notedIdMap.getOrDefault(itemId, itemId);
        for (final Item item : container.getItems())
        {

            if (originalID == notedIdMap.getOrDefault(item.getId(), item.getId()))
            {
                total += item.getQuantity();
            }
        }
        return total;
    }

    // Returns the ID for the first item in the inventory, or -1 if none found
    public static int getFirstItem(final int inventoryId)
    {
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null || container.count() == 0)
        {
            return -1;
        }
        return Arrays.stream(container.getItems()).findFirst().get().getId();
    }

    // Returns true if every item in the inventory has the same ID. Noted items are treated as their un-noted versions.
    public static boolean hasOneTypeOfItem(final int inventoryId)
    {
        if (isCurrencyOnly(inventoryId))
        {
            return true;
        }
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null)
        {
            return false;
        }
        int id = -1;
        for (final Item item : container.getItems())
        {
            if (id == -1)
            {
                id = notedIdMap.getOrDefault(item.getId(), item.getId());
            }
            else if (id != notedIdMap.getOrDefault(item.getId(), item.getId()))
            {
                return false;
            }
        }
        return id != -1;
    }

    // Returns true if the inventory only contains coins and platinum. Returns false if empty.
    public static boolean isCurrencyOnly(final int inventoryId)
    {
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null || container.count() == 0)
        {
            return false;
        }
        for (final Item item : container.getItems())
        {
            if (!isCurrency(item.getId()))
            {
                return false;
            }
        }
        return true;
    }

    // Returns the total value of all coins and platinum chips in the inventory.
    public static long getTotalCurrencyValue(final int inventoryId)
    {
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null || container.count() == 0)
        {
            return 0L;
        }
        long sum = 0;
        for (final Item item : container.getItems())
        {
            switch (item.getId())
            {
                case ItemID.COINS:
                    sum +=  item.getQuantity(); break;
                case ItemID.PLATINUM:
                    sum += (item.getQuantity() * 1000L); break;
            }
        }
        return sum;
    }

    // Analyzes the trade and returns whether the players is selling one type of item, buying one type of item or is more complex (INVALID)
    public static OfferType getOfferType(final int itemId)
    {
        final boolean offerCurrency = isCurrency(itemId);
        final boolean otherCurrency = isCurrencyOnly(TRADEOTHER);
        if (offerCurrency == otherCurrency)
        {
            return OfferType.INVALID;
        }
        final boolean oneTypeReceived = hasOneTypeOfItem(TRADEOTHER);
        if (offerCurrency && !oneTypeReceived)
        {
            return OfferType.INVALID;
        }
        return offerCurrency ? OfferType.BUY : OfferType.SELL;
    }

    // Given a widget, tries to find a valid item ID in it or its child widgets
    public static int getItemIdFromWidget(Widget w)
    {
        int itemId = w.getItemId();
        if (itemId == -1 && w.getChildren() != null)
        {
            for (final Widget child : w.getChildren())
            {
                if (child.getItemId() != -1)
                {
                    return child.getItemId();
                }
            }
        }
        return itemId;
    }

    // Caches the relationship between noted and un-noted IDs for any new items from the passed inventory
    public static void updateNotedIds(final int inventoryId)
    {
        final ItemContainer container = client.getItemContainer(inventoryId);
        if (container == null || container.count() == 0)
        {
            return;
        }
        for (final Item item : container.getItems())
        {
            if (notedIdMap.containsKey(item.getId()))
            {
                continue;
            }
            var comp = itemManager.getItemComposition(item.getId());
            notedIdMap.put(item.getId(), comp.getNote() == -1 ? item.getId() : comp.getLinkedNoteId());
        }
    }

    public static void chatMessage(final boolean notify, final String message)
    {
        final ChatMessageBuilder messageBuilder = new ChatMessageBuilder()
                .append(message);
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(messageBuilder.build())
                .build());
        if (notify)
        {
            notifier.notify(message);
        }
    }

}
