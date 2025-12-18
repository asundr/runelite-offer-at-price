package org.asundr;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;

import java.util.Objects;

@Slf4j
public class TradeCalculatorManager
{
    public static final int WIDGET_ID_TEXT_ENTRY = 162;
    public static final int WIDGET_CHILD_ID_TEXT_ENTRY = 42;
    public static final int TRADE_MENU = 335;
    public static final String TEXT_OFFER_X = "Offer-X";
    public static final String TEXT_OFFER_PRICE_X = "Offer Price X";

    private static Client client;
    private final KeyManager keyManager;
    private final EventBus eventBus;

    private final PriceKeyListener priceKeyListener;
    @Getter private int activeItemID = -1;
    @Getter private OfferType activeOfferType = OfferType.INVALID;

    public TradeCalculatorManager(final Client client, final KeyManager keyManager, final ClientThread clientThread, final EventBus eventBus, final OfferAtPriceConfig config)
    {
        TradeCalculatorManager.client = client;
        this.keyManager = keyManager;
        this.priceKeyListener = new PriceKeyListener(client, clientThread, this, config);
        this.eventBus = eventBus;
        this.priceKeyListener.setOnSubmitted( () ->
        {
            this.keyManager.unregisterKeyListener(this.priceKeyListener);
            this.eventBus.unregister(this.priceKeyListener);
        });
    }

    @Subscribe
    private void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == PriceUtils.TRADEOTHER)
        {
            PriceUtils.updateNotedIds(PriceUtils.TRADEOTHER);
        }
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == TRADE_MENU)
        {
            PriceUtils.updateNotedIds(InventoryID.INV);
            PriceUtils.updateNotedIds(InventoryID.TRADEOFFER);
            PriceUtils.updateNotedIds(PriceUtils.TRADEOTHER);
        }
    }

    @Subscribe
    private void onWidgetClosed(WidgetClosed event)
    {
        if (event.getGroupId() == WIDGET_ID_TEXT_ENTRY)
        {
            keyManager.unregisterKeyListener(priceKeyListener);
            eventBus.unregister(priceKeyListener);
        }
    }

    @Subscribe
    public void onMenuOpened(final MenuOpened event)
    {
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
//            final boolean isValidSource = group == InterfaceID.TRADESIDE; // || group == InterfaceID.INVENTORY || group == InterfaceID.EQUIPMENT || group == InterfaceID.TRADEMAIN;
            if (group == InterfaceID.TRADESIDE && TEXT_OFFER_X.equals(PriceUtils.sanitizeWidgetText(entry.getOption())) && entry.getIdentifier() == 5)
            {
                activeItemID = PriceUtils.getItemIdFromWidget(w);
                if (activeItemID == -1)
                {
                    return;
                }
                if (!client.isKeyPressed(KeyCode.KC_SHIFT))
                {
                    return;
                }
                activeOfferType = PriceUtils.getOfferType(activeItemID);
                if (activeOfferType == OfferType.INVALID)
                {
                    return;
                }
                entry.setOption(TEXT_OFFER_PRICE_X);
                entry.onClick(e -> {
                    keyManager.registerKeyListener(priceKeyListener);
                    eventBus.register(priceKeyListener);
                    Objects.requireNonNull(client.getWidget(WIDGET_ID_TEXT_ENTRY, WIDGET_CHILD_ID_TEXT_ENTRY)).setText("Enter a price:");
                });

            }
        }
    }

}
