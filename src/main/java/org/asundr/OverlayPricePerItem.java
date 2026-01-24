package org.asundr;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

import java.awt.*;


public class OverlayPricePerItem extends OverlayPanel
{
    final private static int OFFSET_TRADE_OFFER = -16;
    final private static int OFFSET_TRADE_CONFIRM = -5;
    final private static String TEXT_NOT_SIMPLE = "Not a simple trade";
    final private static String FORMAT_BUYING = "Buying %sat %s ea";
    final private static String FORMAT_SELLING = "Selling %sat %s ea";

    private static OfferAtPriceConfig config;
    private static ClientThread clientThread;
    private static ItemManager itemManager;


    private String priceText = "";
    private String itemName;

    OverlayPricePerItem(OfferAtPriceConfig config, ClientThread clientThread, ItemManager itemManager)
    {
        OverlayPricePerItem.config = config;
        OverlayPricePerItem.clientThread = clientThread;
        OverlayPricePerItem.itemManager = itemManager;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
    }

    @Subscribe
    private void onEventTradeStateChanged(OfferManager.EventTradeStateChanged event)
    {
        if (event.currentState == OfferManager.TradeState.NOT_TRADING)
        {
            priceText = "";
            itemName = "";
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!OfferManager.isTrading() || !config.showPricePerItemOverlay())
        {
            return null;
        }
        if (config.hideOverlayForInvalid() && OfferManager.getTradeType() == OfferManager.TradeType.INVALID)
        {
            return null;
        }
        updateWarningString();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(priceText)
                .color(config.colorOfPriceOverlay())
                .build());
        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(priceText) + 10,
                40));
        final Rectangle rect = OfferManager.getTradeMenuLocation();
        final int yOffset = OfferManager.getTradeState() == OfferManager.TradeState.TRADE_OFFER ? OFFSET_TRADE_OFFER : OFFSET_TRADE_CONFIRM;
        setPreferredLocation(new java.awt.Point((int)rect.getX() + rect.width/2 - getBounds().width/2, yOffset + (int)rect.getY() + rect.height));
        return super.render(graphics);
    }

    private void updateWarningString()
    {
        switch (OfferManager.getTradeType())
        {
            case SELLING:
                priceText = FORMAT_SELLING;break;
            case BUYING:
                priceText = FORMAT_BUYING; break;
            case INVALID:
                priceText = TEXT_NOT_SIMPLE; break;
        }
        if (OfferManager.getTradeType() != OfferManager.TradeType.INVALID)
        {
            final OfferManager.OfferInfo tradeInfo = OfferManager.getOfferInfo();
            float price = tradeInfo.price;
            price = price < 100f ? Math.round(1000*price)/1000.f : price < 10000 ? Math.round(10*price)/10.f : Math.round(price);
            if (config.showItemNameInOverlay())
            {
                clientThread.invoke(() -> {
                    itemName = itemManager.getItemComposition(tradeInfo.itemId).getMembersName();
                });
            }
            priceText = String.format(priceText, config.showItemNameInOverlay() ? itemName + " " : "", QuantityFormatter.formatNumber(price));
        }
    }

}
