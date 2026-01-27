package org.asundr;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.util.Objects;

public class OfferManager
{
    private static final class TradeMenuId
    {
        public static final int TRADE_MENU = 335;
        public static final int TRADE_CONFIRMATION_MENU = 334;
    }
    private static final class TradeContainerId
    {
        public static final int GIVEN = InventoryID.TRADEOFFER;
        public static final int RECEIVED = InventoryID.TRADEOFFER | 0x8000;
    }
    enum TradeState
    {
        NOT_TRADING,
        TRADE_OFFER,
        TRADE_CONFIRM;
    }

    static class OfferInfo
    {
        int itemId = -1;
        int inputPrice = 0;
        double price = 0d;
        long priceDifference = 0L;
    }

    static class EventTradeStateChanged
    {
        final TradeState previousState;
        final TradeState currentState;
        EventTradeStateChanged(final TradeState previousState, final TradeState currentState)
        {
            this.previousState = previousState;
            this.currentState = currentState;
        }
    }

    static final int WIDGET_ID_TEXT_ENTRY = 162;
    static final int WIDGET_CHILD_ID_TEXT_ENTRY = 42;
    private static final String TEXT_OFFER_X = "Offer-X";
    private static final String TEXT_OFFER_PRICE_X = "Offer-Price-X";

    private static Client client;
    private static ClientThread clientThread;
    private static EventBus eventBus;
    private static OverlayManager overlayManager;
    private static KeyManager keyManager;

    @Getter
    private static TradeState tradeState = TradeState.NOT_TRADING;
    @Getter
    private static TradeType tradeType = TradeType.INVALID;
    @Getter
    private static OfferInfo offerInfo = new OfferInfo();
    private final PriceKeyListener priceKeyListener;

    private static OverlayPricePerItem overlayPricePerItem;
    private static OverlayPriceDifference overlayPriceDifference;


    OfferManager(OfferAtPriceConfig config, Client client, ClientThread clientThread, EventBus eventBus, OverlayManager overlayManager, KeyManager keyManager, ItemManager itemManager)
    {
        OfferManager.client = client;
        OfferManager.clientThread = clientThread;
        OfferManager.eventBus = eventBus;
        OfferManager.overlayManager = overlayManager;
        OfferManager.keyManager = keyManager;

        this.priceKeyListener = new PriceKeyListener(client, clientThread, config);
        keyManager.registerKeyListener(this.priceKeyListener);
        eventBus.register(this.priceKeyListener);
        this.priceKeyListener.setOnSubmitted(() -> this.priceKeyListener.setActive(false));

        overlayPricePerItem = new OverlayPricePerItem(config, clientThread, itemManager);
        overlayPriceDifference = new OverlayPriceDifference(config);
        eventBus.register(this);
        eventBus.register(overlayPricePerItem);
        overlayManager.add(overlayPricePerItem);
        overlayManager.add(overlayPriceDifference);
    }

    public void shutdown()
    {
        keyManager.unregisterKeyListener(this.priceKeyListener);
        eventBus.unregister(this.priceKeyListener);

        overlayManager.remove(overlayPriceDifference);
        overlayManager.remove(overlayPricePerItem);
        eventBus.unregister(overlayPricePerItem);
        eventBus.unregister(this);
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == TradeMenuId.TRADE_MENU)
        {
            updateTradeData();
            PriceUtils.updateNotedIds(InventoryID.INV);
            PriceUtils.updateNotedIds(InventoryID.TRADEOFFER);
            PriceUtils.updateNotedIds(PriceUtils.TRADEOTHER);
            setTradeState(TradeState.TRADE_OFFER);
        }
        else if (event.getGroupId() == TradeMenuId.TRADE_CONFIRMATION_MENU)
        {
            setTradeState(TradeState.TRADE_CONFIRM);
        }
    }

    @Subscribe
    private void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == TradeMenuId.TRADE_CONFIRMATION_MENU)
        {
            setTradeState(TradeState.NOT_TRADING);
        }
        else if (event.getGroupId() == TradeMenuId.TRADE_MENU)
        {
            clientThread.invokeLater(() -> {
                final Widget widget = client.getWidget(TradeMenuId.TRADE_CONFIRMATION_MENU, 0);
                if (widget == null || widget.isHidden())
                {
                    setTradeState(TradeState.NOT_TRADING);
                }
            });
        }
        else if (event.getGroupId() == WIDGET_ID_TEXT_ENTRY)
        {
            priceKeyListener.setActive(false);
        }
    }

    @Subscribe
    private void onItemContainerChanged(ItemContainerChanged event)
    {
        if (tradeState != TradeState.TRADE_OFFER)
        {
            return;
        }
        switch (event.getContainerId())
        {
            case TradeContainerId.RECEIVED:
                PriceUtils.updateNotedIds(PriceUtils.TRADEOTHER);
                updateTradeData();
                break;
            case TradeContainerId.GIVEN:
                updateTradeData();
                break;
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event)
    {
        if (tradeState == TradeState.TRADE_OFFER)
        {
            clientThread.invoke(this::updateTradeData);
        }
    }

    @Subscribe
    private void onMenuOpened(final MenuOpened event)
    {
        if (!client.isKeyPressed(KeyCode.KC_SHIFT))
        {
            return;
        }
        final MenuEntry[] entries = event.getMenuEntries();
        for (int idx = entries.length -1; idx >= 0; --idx)
        {
            final MenuEntry entry = entries[idx];
            final Widget w = entry.getWidget();
            if (w == null)
            {
                return;
            }
            final int group = WidgetUtil.componentToInterface(w.getId());
            if (group == InterfaceID.TRADESIDE && TEXT_OFFER_X.equals(PriceUtils.sanitizeWidgetText(entry.getOption())) && entry.getIdentifier() == 5)
            {
                final int offerItemID = PriceUtils.getItemIdFromWidget(w);
                if (offerItemID == -1)
                {
                    return;
                }
                final TradeType offerType = PriceUtils.getOfferType(offerItemID);
                if (offerType == TradeType.INVALID)
                {
                    return;
                }
                priceKeyListener.setSelectedItemID(offerItemID);
                entry.setOption(TEXT_OFFER_PRICE_X);
                entry.onClick(e -> {
                    priceKeyListener.setActive(true);
                    Objects.requireNonNull(client.getWidget(WIDGET_ID_TEXT_ENTRY, WIDGET_CHILD_ID_TEXT_ENTRY)).setText("Enter a price:");
                });

            }
        }
    }

    private void updateTradeData()
    {
        final boolean isGivenCurrency = PriceUtils.isCurrencyOnly(InventoryID.TRADEOFFER), isReceivedCurrency = PriceUtils.isCurrencyOnly(PriceUtils.TRADEOTHER);
        int currencyTradeId = 0, itemTradeId = 0;
        if (isGivenCurrency && !isReceivedCurrency && PriceUtils.hasOneTypeOfItem(PriceUtils.TRADEOTHER))
        {
            tradeType = TradeType.BUYING;
            currencyTradeId = InventoryID.TRADEOFFER;
            itemTradeId = PriceUtils.TRADEOTHER;
        }
        else if (isReceivedCurrency && !isGivenCurrency && PriceUtils.hasOneTypeOfItem(InventoryID.TRADEOFFER))
        {
            tradeType = TradeType.SELLING;
            currencyTradeId = PriceUtils.TRADEOTHER;
            itemTradeId = InventoryID.TRADEOFFER;
        }
        else
        {
            tradeType = TradeType.INVALID;
        }
        if (tradeType != TradeType.INVALID)
        {
            offerInfo.itemId = PriceUtils.getFirstItem(itemTradeId);
            final long totalCurrency = PriceUtils.getTotalCurrencyValue(currencyTradeId);
            final long totalItemQuantity = PriceUtils.getQuantity(itemTradeId, offerInfo.itemId);
            offerInfo.price = (double)totalCurrency / (double)totalItemQuantity;
            if (offerInfo.inputPrice != 0)
            {
                offerInfo.priceDifference = offerInfo.inputPrice * totalItemQuantity - totalCurrency;
            }
        }
    }

    private void setTradeState(TradeState tradeState)
    {
        if (OfferManager.tradeState == tradeState)
        {
            return;
        }
        final TradeState prevState = OfferManager.tradeState;
        OfferManager.tradeState = tradeState;
        eventBus.post(new EventTradeStateChanged(prevState, tradeState));
        if (tradeState == TradeState.NOT_TRADING)
        {
            offerInfo = new OfferInfo();
            priceKeyListener.setSelectedItemID(-1);
        }
    }

    public static boolean isTrading() { return tradeState != TradeState.NOT_TRADING; }

    public static Rectangle getTradeMenuLocation()
    {
        Widget w = client.getWidget(TradeMenuId.TRADE_MENU, 0);
        if (w == null)
        {
            w = client.getWidget(TradeMenuId.TRADE_CONFIRMATION_MENU, 0);
        }
        if (w == null)
        {
            return  new Rectangle(-1, -1);
        }
        return w.getBounds();
    }

    public static void setInputPrice(final int inputPrice)
    {
        offerInfo.inputPrice = inputPrice;
    }

}
