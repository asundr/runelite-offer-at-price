package org.asundr;

import net.runelite.api.ChatMessageType;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

import java.awt.*;
import java.util.HashMap;

public class OverlayLastMessage extends OverlayPanel
{
    private static final int OFFSET_TRADE_OFFER = -10;
    private static final int OFFSET_TRADE_CONFIRM = -25;
    private static OfferAtPriceConfig config;
    private static final HashMap<String, MessageNode> lastMessages = new HashMap<>();

    private String lastMessage = null;

    OverlayLastMessage(OfferAtPriceConfig config)
    {
        OverlayLastMessage.config = config;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
    }

    @Subscribe
    private void onEventTradeStateChanged(OfferManager.EventTradeStateChanged event)
    {
        if (event.currentState == OfferManager.TradeState.NOT_TRADING)
        {
            lastMessage = null;
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage evt)
    {
        final String eventName = Text.removeTags(evt.getName());
        if (eventName.isEmpty())
        {
            return;
        }
        lastMessages.put(eventName, evt.getMessageNode());
        final String playerName = OfferManager.getOfferInfo().playerName;
        if (!eventName.equals(playerName))
        {
            return;
        }
        lastMessage = Text.removeTags(evt.getMessage());
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (lastMessage == null || !config.showLastChat() || !OfferManager.isTrading())
        {
            return null;
        }
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(lastMessage)
                .color(config.colorOfLastChatOverlay())
                .build());
        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(lastMessage) + 10,
                40));
        final Rectangle rect = OfferManager.getTradeMenuLocation();
        final int yOffset = OfferManager.getTradeState() == OfferManager.TradeState.TRADE_OFFER ? OFFSET_TRADE_OFFER : OFFSET_TRADE_CONFIRM;
        setPreferredLocation(new java.awt.Point((int)rect.getX() + rect.width/2 - getBounds().width/2, yOffset + (int)rect.getY()));
        return super.render(graphics);
    }

    public void onTradedPlayerNameChanged()
    {
        final String playerName = OfferManager.getOfferInfo().playerName;
        if (playerName == null)
        {
            return;
        }
        final MessageNode lastMessageNode = lastMessages.get(playerName);
        if (lastMessageNode != null)
        {
            lastMessage = Text.removeTags(lastMessageNode.getValue()).trim();
        }
    }
}
