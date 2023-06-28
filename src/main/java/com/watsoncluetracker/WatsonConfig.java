package com.watsoncluetracker;

import static com.watsoncluetracker.WatsonConfig.ShowItemOverlay.*;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WatsonPlugin.CONFIG_KEY)
public interface WatsonConfig extends Config
{
	@ConfigItem(
		keyName = "showClueScrollTooltip",
		name = "Clue tooltip",
		description = "Show tooltip on clues listing which clues Watson has.",
		position = 1
	)
	default boolean showClueScrollTooltip()
	{
		return true;
	}

	enum ShowItemOverlay {
		WATSON_HAS_CLUE,
		WATSON_NEEDS_CLUE,
		BOTH,
		NEVER
	}

	String WHEN_TO_SHOW_ITEM_OVERLAY_KEY = "whenToShowItemOverlay";
	@ConfigItem(
		keyName = WHEN_TO_SHOW_ITEM_OVERLAY_KEY,
		name = "Item Overlay",
		description = "When to show the 'w' icon on clues in the inventory. If watson needs the clue, it will show in gray, if he has the clue it will show in light blue.",
		position = 2
	)
	default ShowItemOverlay whenToShowItemOverlay()
	{
		return BOTH;
	}

	@ConfigItem(
		keyName = "watsonNeedsClueColor",
		name = "Watson needs clue",
		description = "The color for the item overlay to show that watson needs the clue.",
		position = 3
	)
	default Color watsonNeedsClueColor()
	{
		return Color.LIGHT_GRAY.darker();
	}

	@ConfigItem(
		keyName = "watsonHasClueColor",
		name = "Watson has clue",
		description = "The color for the item overlay to show that watson already has the clue.",
		position = 4
	)
	default Color watsonHasClueColor()
	{
		return new Color(0x59a8eb);
	}

	@ConfigItem(
		keyName = "watsonChatCommand",
		name = "::watson",
		description = "Enables ::watson command to check which clues Watson is holding on to.",
		position = 5
	)
	default boolean watsonChatCommand()
	{
		return true;
	}
}
