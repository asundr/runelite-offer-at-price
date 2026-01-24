package org.asundr;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;

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

    enum TradeType
    {
        INVALID,
        SELLING,
        BUYING
    }

    static class OfferInfo
    {
        int itemId = -1;
        float price = 0f;
        long priceDifference;
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

    //private static OfferAtPriceConfig config;
    private static Client client;
    private static ClientThread clientThread;
    private static EventBus eventBus;
    private static OverlayManager overlayManager;

    @Getter
    private static TradeState tradeState = TradeState.NOT_TRADING;
    @Getter
    private static TradeType tradeType = TradeType.INVALID;
    @Getter
    private static OfferInfo offerInfo = new OfferInfo();

    private static OverlayPricePerItem overlayPricePerItem;


    OfferManager(OfferAtPriceConfig config, Client client, ClientThread clientThread, EventBus eventBus, OverlayManager overlayManager, ItemManager itemManager)
    {
        OfferManager.client = client;
        OfferManager.clientThread = clientThread;
        OfferManager.eventBus = eventBus;
        OfferManager.overlayManager = overlayManager;
        overlayPricePerItem = new OverlayPricePerItem(config, clientThread, itemManager);
        eventBus.register(this);
        eventBus.register(overlayPricePerItem);
        overlayManager.add(overlayPricePerItem);
    }

    public void shutdown()
    {
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
            case TradeContainerId.GIVEN: case TradeContainerId.RECEIVED:
            updateTradeData();
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
            final int id = PriceUtils.getFirstItem(itemTradeId);
            final long totalCurrency = PriceUtils.getTotalCurrencyValue(currencyTradeId);
            final long totalItemQuantity = PriceUtils.getQuantity(itemTradeId, id);
            offerInfo.price = (float)totalCurrency / (float)totalItemQuantity;
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

}
