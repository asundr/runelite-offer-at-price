package org.asundr;

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

import java.awt.*;

public class OverlayPriceDifference extends OverlayPanel
{
    private static final String FORMAT_REMOVE_COINS = "Remove %s gp  (%s ea)";
    private static final String FORMAT_ADD_COINS = "Add %s gp  (%s ea)";
    private static final int OFFSET_Y = 120;
    private static OfferAtPriceConfig config;

    OverlayPriceDifference(OfferAtPriceConfig config)
    {
        OverlayPriceDifference.config = config;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showPriceDifference())
        {
            return null;
        }
        if (OfferManager.getTradeState() != OfferManager.TradeState.TRADE_OFFER || OfferManager.getTradeType() == TradeType.INVALID)
        {
            return null;
        }
        final OfferManager.OfferInfo offerInfo = OfferManager.getOfferInfo();
        if (offerInfo.priceDifference == 0L)
        {
            return null;
        }
        String differenceText;
        if (offerInfo.inputPrice == 0)
        {
            if (!config.showPriceDifferenceNotSet())
            {
                return null;
            }
            differenceText = "No price per item set.";
        }
        else
        {
            differenceText = String.format(offerInfo.priceDifference < 0 ? FORMAT_REMOVE_COINS : FORMAT_ADD_COINS,
                    QuantityFormatter.formatNumber(Math.abs(offerInfo.priceDifference)), QuantityFormatter.formatNumber(OfferManager.getOfferInfo().inputPrice) );
        }
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(differenceText)
                .color(config.colorOfDifferenceOverlay())
                .build());
        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(differenceText) + 10,
                40));
        final Rectangle rect = OfferManager.getTradeMenuLocation();
        final int xOffset = OfferManager.getTradeType() == TradeType.BUYING ? -6*rect.width/8 : rect.width/8 - 10;
        setPreferredLocation(new java.awt.Point((int)rect.getX() + rect.width/2 + xOffset,  (int)rect.getY() + OFFSET_Y));
        return super.render(graphics);
    }
}
