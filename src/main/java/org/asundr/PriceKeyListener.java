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

import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.util.QuantityFormatter;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.Objects;

public class PriceKeyListener implements KeyListener
{
    //private static int VARCLIENTSTR_INPUT_TEXT = 359; //VarClientStr.INPUT_TEXT
    private static final String TEMPLATE_PRICE_PROMPT = "Enter a price: (will give %s)";
    private final Client client;
    private final ClientThread clientThread;
    private final OfferAtPriceConfig config;

    private final TradeCalculatorManager tradeCalculatorManager;

    @Setter private Runnable onSubmitted;
    private String lastInputText;


    public PriceKeyListener(final Client client, final ClientThread clientThread, final TradeCalculatorManager tradeCalculatorManager, final OfferAtPriceConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.tradeCalculatorManager = tradeCalculatorManager;
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        lastInputText = client.getVarcStrValue(VarClientID.MESLAYERINPUT).toLowerCase();
        final Widget promptWidget = client.getWidget(TradeCalculatorManager.WIDGET_ID_TEXT_ENTRY, TradeCalculatorManager.WIDGET_CHILD_ID_TEXT_ENTRY);
        if (promptWidget != null)
        {
            promptWidget.setText(String.format(TEMPLATE_PRICE_PROMPT, getOutputQuantity()));
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        lastInputText = client.getVarcStrValue(VarClientID.MESLAYERINPUT).toLowerCase();
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            handleInput();
        }
        else
        {
            Objects.requireNonNull(client.getWidget(TradeCalculatorManager.WIDGET_ID_TEXT_ENTRY, TradeCalculatorManager.WIDGET_CHILD_ID_TEXT_ENTRY))
                    .setText(String.format(TEMPLATE_PRICE_PROMPT, "..."));
        }
    }

    private void handleInput()
    {
        clientThread.invoke(() -> {
            client.setVarcStrValue(VarClientID.MESLAYERINPUT, Long.toString(getOutputQuantity()));
            if (onSubmitted != null)
            {
                onSubmitted.run();
            }
            lastInputText = "";
        });
    }

    // Analyzes the current trade type and returns a quantity based on the price the player has entered
    private long getOutputQuantity()
    {
        final String inputText = lastInputText;
        if (!inputText.matches("[0-9]+(?:\\.[0-9]+[kmb])?"))
        {
            return 0;
        }
        final String transformedPrice = transformDecimalPrice(inputText);
        final OfferType offertype = PriceUtils.getOfferType(tradeCalculatorManager.getActiveItemID());
        if (offertype == OfferType.INVALID)
        {
            return 0;
        }
        final int inputPricePerItem = Math.max(0, Integer.parseInt(transformedPrice));
        long outNum = 0;
        if (offertype == OfferType.SELL)
        {
            if (inputPricePerItem != 0)
            {
                final long receivedCurrency = PriceUtils.getTotalCurrencyValue(PriceUtils.TRADEOTHER);
                final long alreadyOfferedItems = PriceUtils.getQuantity(InventoryID.TRADEOFFER, tradeCalculatorManager.getActiveItemID());
                final long sellCount = config.defaultRoundingMethod().method.apply((float)receivedCurrency / (float)inputPricePerItem) - alreadyOfferedItems;
                final long inventoryCurrency = PriceUtils.getTotalCurrencyValue(InventoryID.INV);
                if (sellCount < 0)
                {
                    PriceUtils.chatMessage(config.notifyNeedToRemove(),
                            String.format("[Offer at Price] You need to remove %s from your current offer to match at the provided price (%s)",
                                    QuantityFormatter.formatNumber(-sellCount),
                                    QuantityFormatter.formatNumber(inputPricePerItem)));
                }
                else if (inventoryCurrency < sellCount)
                {
                    PriceUtils.chatMessage(config.notifyNotEnough(),
                            String.format("[Offer at Price] You don't have enough items (missing %s) to match at the provided price (%s)",
                                    QuantityFormatter.formatNumber(sellCount-inventoryCurrency),
                                    QuantityFormatter.formatNumber(inputPricePerItem)));
                }
                outNum = Math.max(0, Math.min(Integer.MAX_VALUE, sellCount));
            }
        }
        else // BUY
        {
            final long receivedQuantity = PriceUtils.getQuantity(PriceUtils.TRADEOTHER, PriceUtils.getFirstItem(PriceUtils.TRADEOTHER));
            final long alreadyOfferedCurrency = PriceUtils.getTotalCurrencyValue(InventoryID.TRADEOFFER);
            final long offerQuantity = inputPricePerItem * receivedQuantity - alreadyOfferedCurrency;
            final long inventoryQuantity = PriceUtils.getQuantity(InventoryID.INV, tradeCalculatorManager.getActiveItemID());
            if (offerQuantity < 0)
            {
                PriceUtils.chatMessage(config.notifyNeedToRemove(),
                        String.format("[Offer at Price] You need to remove %sgp from your current offer to match at the provided price (%s)",
                                QuantityFormatter.formatNumber(-offerQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem)));
            }
            else if (inventoryQuantity < offerQuantity)
            {
                PriceUtils.chatMessage(config.notifyNotEnough(),
                        String.format("[Offer at Price] You don't have enough coins (missing %s) to match at the provided price (%s)",
                                QuantityFormatter.formatNumber(offerQuantity-inventoryQuantity),
                                QuantityFormatter.formatNumber(inputPricePerItem)));
            }
            outNum = Math.max(0, Math.min(offerQuantity, Integer.MAX_VALUE));
        }
        return outNum;
    }

    // Adapted from Decimal Prices
    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1_000);
    private static final BigDecimal ONE_MILLION = new BigDecimal(1_000_000);
    private static final BigDecimal ONE_BILLION = new BigDecimal(1_000_000_000);
    private static final BigDecimal MAX = new BigDecimal(2_147_483_647);
    private static String transformDecimalPrice(String decimalPrice)
    {
        // if passed string isn't a decimal return it as-is
        if (!decimalPrice.matches("[0-9]+\\.[0-9]+[kmb]"))
        {
            return decimalPrice;
        }
        int priceStringLen = decimalPrice.length();
        // get the unit from the end of string, k (thousands), m (millions) or b (billions)
        char unit = decimalPrice.charAt(priceStringLen - 1);
        // get the number xx.xx without the unit and parse as a BigDecimal (for precision)
        BigDecimal amount = new BigDecimal(decimalPrice.substring(0, priceStringLen - 1));
        // multiply the number and the unit
        BigDecimal product;
        switch (unit) {
            case 'k':
                product = amount.multiply(ONE_THOUSAND);
                break;
            case 'm':
                product = amount.multiply(ONE_MILLION);
                break;
            case 'b':
                product = amount.multiply(ONE_BILLION);
                break;
            default:
                product = BigDecimal.ZERO;
                break;
        }
        // bound result to maximum allowable price
        if (product.compareTo(MAX) > 0) {
            product = MAX;
        }
        // cast the BigDecimal to an int, truncating anything after the decimal in the process
        int truncatedProduct = product.intValue();
        return String.valueOf(truncatedProduct);
    }

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyReleased(KeyEvent e) { }
}
