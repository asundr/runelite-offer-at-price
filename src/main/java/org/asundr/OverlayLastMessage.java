package org.asundr;

import com.google.common.collect.ImmutableSet;
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
import java.util.Set;

public class OverlayLastMessage extends OverlayPanel
{
    private static final int OFFSET_TRADE_OFFER = -10;
    private static final int OFFSET_TRADE_CONFIRM = -25;
    private static final Set<ChatMessageType> VALID_CHAT_TYPES = ImmutableSet.of(
            ChatMessageType.PUBLICCHAT,
            ChatMessageType.FRIENDSCHAT,
            ChatMessageType.CLAN_CHAT,
            ChatMessageType.PRIVATECHAT,
            ChatMessageType.MODCHAT,
            ChatMessageType.MODPRIVATECHAT,
            ChatMessageType.CLAN_GUEST_CHAT
    );
    private static OfferAtPriceConfig config;
    private static final HashMap<String, MessageNode> lastMessages = new HashMap<>();

    OverlayLastMessage(OfferAtPriceConfig config)
    {
        OverlayLastMessage.config = config;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPreferredColor(Color.GREEN);
        setBounds(new Rectangle(100,100));
    }

    @Subscribe
    private void onChatMessage(ChatMessage evt)
    {
        final String eventName = Text.removeTags(evt.getMessageNode().getName());
        if (!VALID_CHAT_TYPES.contains(evt.getType()))
        {
            return;
        }
        lastMessages.put(eventName, evt.getMessageNode());
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final String playerName = OfferManager.getOfferInfo().playerName;
        if (playerName == null || !config.showLastChat() || !OfferManager.isTrading() || !lastMessages.containsKey(playerName))
        {
            return null;
        }
        final String lastMessage = Text.removeTags(lastMessages.get(playerName).getValue());
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
}
