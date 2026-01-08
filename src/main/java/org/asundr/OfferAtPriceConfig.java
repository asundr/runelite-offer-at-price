/*
 * Copyright (c) 2025, Arun <offer-at-price-plugin.le03k@dralias.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.asundr;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

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

	@ConfigItem(
			keyName = "notifyNeedToRemove", name = "Notify if need to remove", description = "Send notification if you need to remove items from what has been offered at entered price"
	)
	default boolean notifyNeedToRemove() { return true; }

	@ConfigItem(
			keyName = "notifyNotEnough", name = "Notify if not enough", description = "Send notification if you don't have enough items to match the other offer at the entered price"
	)
	default boolean notifyNotEnough() { return true; }

	@ConfigItem(
			keyName = "showPricePerItemOverlay", name = "Price per item overlay", description = "During trades of items for currency, will display the current price per item"
	)
	default boolean showPricePerItemOverlay() { return true; }

	@ConfigItem(
			keyName = "colorOfPriceOverlay", name = "Price overlay color", description = "The color used for the text on the price per item overlay"
	)
	default Color colorOfPriceOverlay() { return new Color(0xB5, 0xE4, 0x93); }

}
