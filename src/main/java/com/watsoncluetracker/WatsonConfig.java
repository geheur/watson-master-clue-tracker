package com.watsoncluetracker;

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

	@ConfigItem(
		keyName = "showClueScrollItemOverlay",
		name = "Clue item overlay",
		description = "Show icon on clues if Watson has a clue of that difficulty.",
		position = 2
	)
	default boolean showClueScrollItemOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watsonChatCommand",
		name = "::watson",
		description = "Enables ::watson command to check which clues Watson is holding on to.",
		position = 3
	)
	default boolean watsonChatCommand()
	{
		return true;
	}
}
