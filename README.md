# [Offer at Price](https://runelite.net/plugin-hub/show/offer-at-price)

This RuneLite plugin provides the player with a way to match a trade offer by providing a price per item.
Additionally, shows current price per item overlay during trades and an overlay for how much over or under a trade is from perfectly matching the provided price.

![image](https://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/installs/plugin/offer-at-price)
![image](https://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/rank/plugin/offer-at-price)

**Even when using this plugin, it is your responsibility to verify the accuracy of your trades.**

### Features
- Offer and remove items/coins in trades by providing a price per item (Shift+right-click)
- Can manually set the price by right-clicking items in trade window
- Calculates and displays the current price being paid for each item below the trade window
- Displays by how much the trade is being over- or underpaid given the provided price
- Displays the last chat message of the currently traded player above the trade window

<hr>

### Detailed Steps to Offer-at-price-X
1) Wait until the other player has submitted their offer
2) SHIFT-right click on the item you plan to trade in return (coins for items, or items for coins)
3) Select the option to "Offer at Price <_Selected Item_>"
4) Enter the price of the item being given or received
5) Submitting this value will calculate number of coins or items needed to match the received items

### Conditions required to Offer-at-price-X
- OTHER player offers **N** items.
  - You must SHIFT-right click your coins and enter the **Price** per item.
  - This will offer N x Price coins.
- OTHER player offers **N** coins.
  - You must SHIFT-right click the traded item and enter the **Price** per item.
  - This will offer N / Price items.

If the option to "Offer-at-price-X" doesn't appear when SHIFT+right-clicking an item in your inventory, the following may be the cause:
  - The other player hasn't offered any items yet
  - The other player has offered more than one type of item
  - The other player offered currency, and you're also trying to offer currency
  - The other player offered non-currency items, and you're also trying to offer non-currency items

<hr>

### Alerts for bad input
Submitting invalid prices will alert you via the chat box and optionally with a notification:
* At the submitted price, you have already offered too many items to match and will need to manually remove some
* At the submitted price, you don't have enough items to match the other player's offer

### Overlays
* **Price per item**: displays how many coins is being paid per item below the trade window
* **Price difference**: shows how many items need to be added/removed to match the input price
* **Last chat**: shows the most recent chat message from the currently traded player above the trade window

<hr>

### Notes
- Items can be removed by price by SHIFT right-clicking on already offered items
- If you have already offered items, the plugin will only add the difference to match the opposing offer
- Make sure you have enough items to actually cover the item price you provide
- When offering items, it's possible that the price won't divide perfectly into the coins offered by the other player. A config option is provided to determine if this value should be rounded up, down or to nearest.
- This plugin is intended for simple trades of one type of item for currency and doesn't support multiple item types.
- There is _limited_ support for receiving payments with platinum chips but offers must be made with coins.
<hr>

### My RuneLite plugins
- [Trade Tracker](https://runelite.net/plugin-hub/show/trade-tracker) - Records a detailed, searchable history of past trades with other players
- [Offer at Price](https://runelite.net/plugin-hub/show/offer-at-price) - SHIFT-right click an item to offer a matching quantity for a provided price
- [Smithing MisClick Prevention](https://runelite.net/plugin-hub/show/offer-at-price) - Prevents accidentally clicking on unintended items in the smithing menu
- [Ring of Forging Helper](https://runelite.net/plugin-hub/show/ring-of-forging-helper) - Provides customizable feedback to make smithing with the Ring of Forging more afk
- [Chat Space Blocker](https://runelite.net/plugin-hub/show/chat-space-blocker) - Prevents players from entering spaces as the first character of chat input
- [Trade Highlighter](https://runelite.net/plugin-hub/show/trade-highlighter) - Highlights configurable items in the player trade menu. Useful for scam prevention.
