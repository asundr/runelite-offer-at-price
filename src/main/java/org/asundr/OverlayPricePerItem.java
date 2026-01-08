package org.asundr;

import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;


public class OverlayPricePerItem extends OverlayPanel
{
    private static final class TradeMenuId
    {
        public static final int TRADE_MENU = 335;
        public static final int TRADE_CONFIRMATION_MENU = 334;
    }

    private static OfferAtPriceConfig config;
    private static ClientThread clientThread;
    private static Client client;
    private boolean isActive = false;
    private Rectangle tradeWidgetBounds = new Rectangle(-1, -1);

    OverlayPricePerItem(Plugin plugin, OfferAtPriceConfig config, ClientThread clientThread, Client client)
    {
        OverlayPricePerItem.config = config;
        OverlayPricePerItem.clientThread = clientThread;
        OverlayPricePerItem.client = client;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
    }

    @Subscribe
    private void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == TradeMenuId.TRADE_CONFIRMATION_MENU)
        {
            isActive = false;
        }
        else if (event.getGroupId() == TradeMenuId.TRADE_MENU)
        {
            clientThread.invokeLater(() -> {
                final Widget widget = client.getWidget(TradeMenuId.TRADE_CONFIRMATION_MENU);
                if (widget == null || widget.isHidden())
                {
                    isActive = false;
                }
            });
        }
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == TradeMenuId.TRADE_MENU)
        {
            isActive = true;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final Rectangle rect = updateTradeMenuLocation();

        setPreferredLocation(new java.awt.Point((int)rect.getX() + (int)rect.width/2, (int)rect.getY() + (int)(rect.height)));

        System.out.println(String.format("Widget location: %s, %s", rect.getX(), rect.getY()));

        if (!isActive || !config.showPricePerItemOverlay())
        {
            final String warningString = "WARNING: NOT WEARING RING OF FORGING";
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text(warningString)
                    .color(Color.WHITE)
                    .build());
            panelComponent.setPreferredSize(new Dimension(
                    graphics.getFontMetrics().stringWidth(warningString) + 10,
                    50));
            setPreferredLocation(new java.awt.Point((int)rect.getX() + (int)rect.width/2 - getBounds().width/2, 5 + (int)rect.getY() + (int)(rect.height)));
            return super.render(graphics);
        }

        // add price per item overlay
        final boolean isGivenCurrency = PriceUtils.isCurrencyOnly(InventoryID.TRADEOFFER), isReceivedCurrency = PriceUtils.isCurrencyOnly(PriceUtils.TRADEOTHER);
        String warningString = "";
        int currencyTradeId = 0, itemTradeId = 0;
        if (isGivenCurrency && !isReceivedCurrency && PriceUtils.hasOneTypeOfItem(PriceUtils.TRADEOTHER))
        {
            currencyTradeId = InventoryID.TRADEOFFER;
            itemTradeId = PriceUtils.TRADEOTHER;
            warningString = "Buying at %s ea";
        }
        else if (isReceivedCurrency && !isGivenCurrency && PriceUtils.hasOneTypeOfItem(InventoryID.TRADEOFFER))
        {
            currencyTradeId = PriceUtils.TRADEOTHER;
            itemTradeId = InventoryID.TRADEOFFER;
            warningString = "Selling at %s ea";
        }
        else
        {
            //System.out.println(String.format("gCurr: %s | rCurr: %s | gOneType: %s | rOneType: %s", isGivenCurrency, isReceivedCurrency, PriceUtils.hasOneTypeOfItem(InventoryID.TRADEOFFER), PriceUtils.hasOneTypeOfItem(PriceUtils.TRADEOTHER)));
            return null;
        }

        final int id = PriceUtils.getFirstItem(itemTradeId);
        final float price = (float)PriceUtils.getTotalCurrencyValue(currencyTradeId) / (float)PriceUtils.getQuantity(itemTradeId, id);
        warningString = String.format(warningString, price);
        System.out.println(warningString);
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(warningString)
                .color(Color.WHITE)
                .build());
        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(warningString) + 10,
                50));
        setPreferredLocation(new java.awt.Point((int)rect.getX() + (int)rect.width/2 - getBounds().width/2, 5 + (int)rect.getY() + (int)(rect.height)));
        return super.render(graphics);
    }

    private Rectangle updateTradeMenuLocation()
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
