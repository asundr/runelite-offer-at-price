package org.asundr;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Offer at Price",
	description = "SHIFT-right click an item to offer a matching quantity for a provided price",
	tags = {"trade", "auto", "calc", "calculate", "value"}
)
public class OfferAtPricePlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OfferAtPriceConfig config;
	@Inject private EventBus eventBus;
	@Inject private KeyManager keyManager;
	@Inject private ItemManager itemManager;

	private TradeCalculatorManager tradeCalculatorManager;

	@Override
	protected void startUp() throws Exception
	{
		PriceUtils.initialize(client, itemManager);
		tradeCalculatorManager = new TradeCalculatorManager(client, keyManager, clientThread, eventBus, config);
		eventBus.register(tradeCalculatorManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(tradeCalculatorManager);
	}

	@Provides
	OfferAtPriceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OfferAtPriceConfig.class);
	}
}
