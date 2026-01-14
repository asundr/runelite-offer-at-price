package org.asundr;

import net.runelite.api.Client;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

import java.awt.*;


public class OverlayPricePerItem extends OverlayPanel
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
    private enum TradeState
    {
        NOT_TRADING,
        TRADE_OFFER,
        TRADE_CONFIRM;
    }

    final private static int OFFSET_TRADE_OFFER = -16;
    final private static int OFFSET_TRADE_CONFIRM = -5;
    final private static String TEXT_NOT_SIMPLE = "Not a simple trade";
    final private static String FORMAT_BUYING = "Buying %sat %s ea";
    final private static String FORMAT_SELLING = "Selling %sat %s ea";

    private static OfferAtPriceConfig config;
    private static ClientThread clientThread;
    private static Client client;
    private static ItemManager itemManager;

    private TradeState tradeState = TradeState.NOT_TRADING;
    private String priceText = "";
    private String itemName;


    OverlayPricePerItem(OfferAtPriceConfig config, ClientThread clientThread, Client client, ItemManager itemManager)
    {
        OverlayPricePerItem.config = config;
        OverlayPricePerItem.clientThread = clientThread;
        OverlayPricePerItem.client = client;
        OverlayPricePerItem.itemManager = itemManager;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
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
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == TradeMenuId.TRADE_MENU)
        {
            updateWarningString();
            setTradeState(TradeState.TRADE_OFFER);
        }
        else if (event.getGroupId() == TradeMenuId.TRADE_CONFIRMATION_MENU)
        {
            setTradeState(TradeState.TRADE_CONFIRM);
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
                updateWarningString();
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event)
    {
        if (tradeState == TradeState.TRADE_OFFER)
        {
            clientThread.invoke(this::updateWarningString);
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!isTrading() || !config.showPricePerItemOverlay())
        {
            return null;
        }
        if (config.hideOverlayForInvalid() && priceText.equals(TEXT_NOT_SIMPLE))
        {
            return null;
        }
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(priceText)
                .color(config.colorOfPriceOverlay())
                .build());
        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(priceText) + 10,
                40));
        final Rectangle rect = updateTradeMenuLocation();
        final int yOffset = tradeState == TradeState.TRADE_OFFER ? OFFSET_TRADE_OFFER : OFFSET_TRADE_CONFIRM;
        setPreferredLocation(new java.awt.Point((int)rect.getX() + rect.width/2 - getBounds().width/2, yOffset + (int)rect.getY() + rect.height));
        return super.render(graphics);
    }

    private static Rectangle updateTradeMenuLocation()
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

    private void setTradeState(TradeState tradeState)
    {
        this.tradeState = tradeState;
        if (tradeState == TradeState.NOT_TRADING)
        {
            priceText = "";
            itemName = "";
        }
    }

    private void updateWarningString()
    {
        final boolean isGivenCurrency = PriceUtils.isCurrencyOnly(InventoryID.TRADEOFFER), isReceivedCurrency = PriceUtils.isCurrencyOnly(PriceUtils.TRADEOTHER);
        int currencyTradeId = 0, itemTradeId = 0;
        if (isGivenCurrency && !isReceivedCurrency && PriceUtils.hasOneTypeOfItem(PriceUtils.TRADEOTHER))
        {
            currencyTradeId = InventoryID.TRADEOFFER;
            itemTradeId = PriceUtils.TRADEOTHER;
            priceText = FORMAT_BUYING;
        }
        else if (isReceivedCurrency && !isGivenCurrency && PriceUtils.hasOneTypeOfItem(InventoryID.TRADEOFFER))
        {
            currencyTradeId = PriceUtils.TRADEOTHER;
            itemTradeId = InventoryID.TRADEOFFER;
            priceText = FORMAT_SELLING;
        }
        else
        {
            priceText = TEXT_NOT_SIMPLE;
        }
        if (!priceText.equals(TEXT_NOT_SIMPLE))
        {
            final int id = PriceUtils.getFirstItem(itemTradeId);
            float price = (float)PriceUtils.getTotalCurrencyValue(currencyTradeId) / (float)PriceUtils.getQuantity(itemTradeId, id);
            price = price < 100f ? Math.round(1000*price)/1000.f : price < 10000 ? Math.round(10*price)/10.f : Math.round(price);
            if (config.showItemNameInOverlay())
            {
                clientThread.invoke(() -> {
                    itemName = itemManager.getItemComposition(id).getMembersName();
                });
            }
            priceText = String.format(priceText, config.showItemNameInOverlay() ? itemName + " " : "", QuantityFormatter.formatNumber(price));
        }
    }

    private boolean isTrading() { return tradeState != TradeState.NOT_TRADING; }
}
