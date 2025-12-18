package org.asundr;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(org.asundr.OfferAtPricePlugin.class);
		RuneLite.main(args);
	}
}