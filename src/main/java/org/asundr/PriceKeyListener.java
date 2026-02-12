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

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.QuantityFormatter;

import java.awt.event.KeyEvent;
import java.util.Objects;

public class PriceKeyListener implements KeyListener
{
    enum TransferState { INACTIVE, GIVING, REMOVING; }

    private static final String TEMPLATE_PRICE_PROMPT = "Enter a price: (will %s %s)";
    private static final String TEMPLATE_PRICE_PROMPT_GIVING = String.format(TEMPLATE_PRICE_PROMPT, "give", "%s");
    private static final String TEMPLATE_PRICE_PROMPT_REMOVING = String.format(TEMPLATE_PRICE_PROMPT, "remove", "%s");
    private static final String MESSAGE_PREFIX = "[Offer at Price] ";
    private static final String TEMPLATE_REMOVE_ITEMS = MESSAGE_PREFIX + "You need to remove %s item(s) from your current offer to match %s item(s) at the provided price (%s).";
    private static final String TEMPLATE_ADD_ITEMS = MESSAGE_PREFIX + "You need to add %s item(s) to your current offer to match %s items at the provided price (%s).";
    private static final String TEMPLATE_NOT_ENOUGH_ITEMS = MESSAGE_PREFIX + "You don't have enough items (missing %s) to match at the provided price (%s). Value of your offer: %s.";
    private static final String TEMPLATE_REMOVE_COINS = MESSAGE_PREFIX + "You need to remove %sgp from your current offer to match %sgp worth of items at the provided price (%sgp).";
    private static final String TEMPLATE_ADD_COINS = MESSAGE_PREFIX + "You need to add %sgp to your current offer to match %sgp at the provided price (%sgp).";
    private static final String TEMPLATE_NOT_ENOUGH_COINS = MESSAGE_PREFIX + "You don't have enough coins (missing %sgp) to match at the provided price (%sgp). Can afford %s item(s) at this price.";

    private final Client client;
    private final ClientThread clientThread;
    private final OfferAtPriceConfig config;


    @Setter
    private Runnable onSubmitted;

    @Getter @Setter
    private int selectedItemID = -1;
    @Setter
    private TradeType selectedItemTradeType = TradeType.INVALID;
    private String lastInputText;

    private TransferState transferState = TransferState.INACTIVE;


    // Enables and determines how to interpret quantity data
    void setTransferState(TransferState state)
    {
        if (transferState == state)
        {
            return;
        }
        transferState = state;
    }

    private boolean isActive() { return transferState != TransferState.INACTIVE; }


    public PriceKeyListener(final Client client, final ClientThread clientThread, final OfferAtPriceConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
    }

    @Subscribe
    private void onClientTick(ClientTick event)
    {
        if (!isActive())
        {
            return;
        }
        lastInputText = client.getVarcStrValue(VarClientID.MESLAYERINPUT).toLowerCase();
        final Widget promptWidget = client.getWidget(OfferManager.WIDGET_ID_TEXT_ENTRY, OfferManager.WIDGET_CHILD_ID_TEXT_ENTRY);
        if (promptWidget != null)
        {
            final String prompt = transferState == TransferState.GIVING ? TEMPLATE_PRICE_PROMPT_GIVING : TEMPLATE_PRICE_PROMPT_REMOVING;
            promptWidget.setText(String.format(prompt, getOutputQuantity()));
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (!isActive())
        {
            return;
        }
        lastInputText = client.getVarcStrValue(VarClientID.MESLAYERINPUT).toLowerCase();
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            handleInput();
        }
        else
        {
            final String prompt = transferState == TransferState.GIVING ? TEMPLATE_PRICE_PROMPT_GIVING : TEMPLATE_PRICE_PROMPT_REMOVING;
            Objects.requireNonNull(client.getWidget(OfferManager.WIDGET_ID_TEXT_ENTRY, OfferManager.WIDGET_CHILD_ID_TEXT_ENTRY))
                    .setText(String.format(prompt, "..."));
        }
    }

    private void handleInput()
    {
        clientThread.invoke(() -> {
            client.setVarcStrValue(VarClientID.MESLAYERINPUT, Long.toString(getOutputQuantity(true)));
            if (onSubmitted != null)
            {
                onSubmitted.run();
            }
            lastInputText = "";
        });
    }

    // Analyzes the current trade type and returns a quantity based on the price the player has entered
    private long getOutputQuantity(final boolean printWarning)
    {
        final String inputText = lastInputText.toLowerCase().trim();
        if (!PriceUtils.isValidPrice(inputText))
        {
            return 0;
        }
        final String transformedPrice = PriceUtils.transformDecimalPrice(inputText);
        final TradeType offerType = PriceUtils.getOfferType(selectedItemID, false);
        if (offerType == TradeType.INVALID)
        {
            return 0;
        }
        final int inputPricePerItem = Math.max(0, Integer.parseInt(transformedPrice));
        OfferManager.setInputPrice(inputPricePerItem);
        long outNum = 0;
        if (offerType == TradeType.SELLING)
        {
            if (inputPricePerItem != 0)
            {
                final long receivedCurrency = PriceUtils.getTotalCurrencyValue(PriceUtils.TRADEOTHER);
                final long alreadyOfferedItems = PriceUtils.getQuantity(InventoryID.TRADEOFFER, selectedItemID);
                final long expectedItemCount = config.defaultRoundingMethod().method.apply((double)receivedCurrency / (double)inputPricePerItem);
                final long transferCount = expectedItemCount - alreadyOfferedItems;
                if (printWarning)
                {
                    printSellingWarning(inputPricePerItem, alreadyOfferedItems, expectedItemCount, transferCount);
                }
                outNum = Math.max(0, Math.min(Integer.MAX_VALUE, transferState == TransferState.GIVING ? transferCount : -transferCount));
            }
        }
        else // BUY (giving currency for items)
        {
            final long receivedQuantity = PriceUtils.getQuantity(PriceUtils.TRADEOTHER, PriceUtils.getFirstItem(PriceUtils.TRADEOTHER));
            final long alreadyOfferedCurrency = PriceUtils.getTotalCurrencyValue(InventoryID.TRADEOFFER);
            final long transferQuantity = inputPricePerItem * receivedQuantity - alreadyOfferedCurrency;
            if (printWarning)
            {
                printBuyingWarning(inputPricePerItem, receivedQuantity, alreadyOfferedCurrency, transferQuantity);
            }
            outNum = Math.max(0, Math.min(Integer.MAX_VALUE, transferState == TransferState.GIVING ? transferQuantity : -transferQuantity));
        }
        return outNum;
    }
    private long getOutputQuantity() {return getOutputQuantity(false); }

    private void printSellingWarning(int inputPricePerItem, long alreadyOfferedItems, long expectedItemCount, long transferCount)
    {
        final long inventoryQuantity = PriceUtils.getQuantity(InventoryID.INV, selectedItemID);
        if (transferState == TransferState.GIVING)
        {
            if (transferCount < 0)
            {
                PriceUtils.chatMessage(config.notifyNeedToRemove(),
                        String.format(TEMPLATE_REMOVE_ITEMS,
                                QuantityFormatter.formatNumber(-transferCount),
                                QuantityFormatter.formatNumber(expectedItemCount),
                                QuantityFormatter.formatNumber(inputPricePerItem)));
            }
            else if (inventoryQuantity < transferCount)
            {
                final long valueOfYourOffer = inputPricePerItem * (alreadyOfferedItems + inventoryQuantity);
                PriceUtils.chatMessage(config.notifyNotEnough(),
                        String.format(TEMPLATE_NOT_ENOUGH_ITEMS,
                                QuantityFormatter.formatNumber(transferCount - inventoryQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem),
                                QuantityFormatter.formatNumber(valueOfYourOffer)));
            }
        }
        else  // Removing
        {
            if (transferCount > 0)
            {
                PriceUtils.chatMessage(config.notifyNeedToAdd(),
                        String.format(TEMPLATE_ADD_ITEMS,
                                QuantityFormatter.formatNumber(expectedItemCount - alreadyOfferedItems),
                                QuantityFormatter.formatNumber(expectedItemCount),
                                QuantityFormatter.formatNumber(inputPricePerItem)));                        }
        }
    }

    private void printBuyingWarning(int inputPricePerItem, long receivedQuantity, long alreadyOfferedCurrency, long transferQuantity)
    {
        final long inventoryCurrency = PriceUtils.getTotalCurrencyValue(InventoryID.INV);
        if (transferState == TransferState.GIVING)
        {
            if (transferQuantity < 0)
            {
                PriceUtils.chatMessage(config.notifyNeedToRemove(),
                        String.format(TEMPLATE_REMOVE_COINS,
                                QuantityFormatter.formatNumber(-transferQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem * receivedQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem)));
            }
            else if (inventoryCurrency < transferQuantity)
            {
                final long canAffordCount = (alreadyOfferedCurrency + inventoryCurrency) / (long)inputPricePerItem;
                PriceUtils.chatMessage(config.notifyNotEnough(),
                        String.format(TEMPLATE_NOT_ENOUGH_COINS,
                                QuantityFormatter.formatNumber(transferQuantity - inventoryCurrency),
                                QuantityFormatter.formatNumber(inputPricePerItem),
                                QuantityFormatter.formatNumber(canAffordCount)));
            }
        }
        else
        {
            if (transferQuantity > 0)
            {
                PriceUtils.chatMessage(config.notifyNeedToAdd(),
                        String.format(TEMPLATE_ADD_COINS,
                                QuantityFormatter.formatNumber(transferQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem * receivedQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem)));
            }
        }
    }

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyReleased(KeyEvent e) { }
}
