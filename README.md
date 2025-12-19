# Offer at Price plugin for RuneLite

This plugin provides the player with a way to match a trade offer by providing a price per item.

**Even when using this plugin, it is your responsibility to verify the accuracy of your trades.**

To use: 
1) Wait until the other player has submitted their offer
2) SHIFT-right click on the item you plan to trade in return (coins for items, or items for coins)
3) Select the option to "Offer at Price <_Selected Item_>"
4) Enter the price of the item being given or received
5) Submitting this value will calculate number of coins or items needed to match the received items

Currently this works for the following trades:
- OTHER player offers **N** items.
  - You must SHIFT-right click your coins and enter the **Price** per item.
  - This will offer N x Price coins.
- OTHER player offers **N** coins.
  - You must SHIFT-right click the traded item and enter the **Price** per item.
  - This will offer N / Price items.

Notes:
- If the option to "Offer at Price" doesn't appear when SHIFT-clicking an item in your inventory, the following may be the cause:
  - The other player hasn't offered any items yet
  - The other player has offered more than one type of item
  - The other player offered currency and you're also trying to offer currency
  - The other player offered non-currency items and you're also trying to offer non-currency items
- Plugin assumes that your offer window is empty. It will not take into account any items that you have already offered.
- Make sure you have enough items to actually cover the item price you provide
- When offering items, it's possible that the price wont divide perfectly into the coins offered by the other player. A config option is provided to determine if this value should be rounded up, down or to nearest.
- This plugin is intended for simple trades of one type of item for currency and doesn't support multiple item types.
- There is _limited_ support for receiving payments with platinum chips but offers must be made with coins.