package org.asundr;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OfferAtPriceConfig.GROUP)
public interface OfferAtPriceConfig extends Config
{
	String GROUP = "OfferAtPrice";

	@ConfigItem(
		keyName = "defaultRoundingMethod",
		name = "Rounding Method",
		description = "When offering items at a price the total value of your items may not match\nthe other player's offer exactly. This determines how your item count is rounded in this case."
	)
	default RoundingRule defaultRoundingMethod()
	{
		return RoundingRule.DOWN;
	}
}
