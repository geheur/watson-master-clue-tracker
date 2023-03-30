package com.watsoncluetracker;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
	name = "Watson Clue Tracker",
	description = "track clues watson is holding on to for you",
	tags = {"clue", "watson", "master"}
)
@Slf4j
public class WatsonPlugin extends Plugin
{
	public static final String CONFIG_KEY = "watsonClueTracker";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WatsonOverlay overlay;

	@Inject
	private WatsonWidgetItemOverlay itemOverlay;

	@Inject
	private ClientThread clientThread;

	@Inject
	private VarbitChanged varbitChanged;

	@Inject
	private WatsonConfig config;

	public static final int CLUE_SCROLL_ITEM_BASE_ID = 713;

	private static final int WATSON_HAS_EASY_VARBIT = 5186;
	private static final int WATSON_HAS_MEDIUM_VARBIT = 5187;
	private static final int WATSON_HAS_HARD_VARBIT = 5188;
	private static final int WATSON_HAS_ELITE_VARBIT = 5189;

	@Provides
	WatsonConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(WatsonConfig.class);
	}

	@Override
	public void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(itemOverlay);
	}

	@Override
	public void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(itemOverlay);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if(config.watsonChatCommand() && "watson".equalsIgnoreCase(commandExecuted.getCommand()))
		{
			chatMessage(generateWatsonNeedsText(" "));
		}
	}

	private void chatMessage(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
	}

	public boolean watsonHasClue(ClueTier tier)
	{
		if(tier == ClueTier.EASY)
		{
			return client.getVarbitValue(WATSON_HAS_EASY_VARBIT) == 1;
		}
		else if(tier == ClueTier.MEDIUM)
		{
			return client.getVarbitValue(WATSON_HAS_MEDIUM_VARBIT) == 1;
		}
		else if(tier == ClueTier.HARD)
		{
			return client.getVarbitValue(WATSON_HAS_HARD_VARBIT) == 1;
		}
		else if(tier == ClueTier.ELITE)
		{
			return client.getVarbitValue(WATSON_HAS_ELITE_VARBIT) == 1;
		}
		return false;
	}

	public String generateWatsonNeedsText(String separator)
	{
		boolean hasEasy = client.getVarbitValue(WATSON_HAS_EASY_VARBIT) == 1;
		boolean hasMedium = client.getVarbitValue(WATSON_HAS_MEDIUM_VARBIT) == 1;
		boolean hasHard = client.getVarbitValue(WATSON_HAS_HARD_VARBIT) == 1;
		boolean hasElite = client.getVarbitValue(WATSON_HAS_ELITE_VARBIT) == 1;
		if (hasEasy && hasMedium && hasHard && hasElite)
		{
			return "Watson is ready to give you a master clue.";
		}
		else
		{
			boolean transparent = client.isResized() && client.getVar(Varbits.TRANSPARENT_CHATBOX) == 1;
			String message = "Watson needs:" + separator;
			if (!hasEasy) message += ColorUtil.wrapWithColorTag("easy", ClueTier.EASY.getColor(transparent)) + separator;
			if (!hasMedium) message += ColorUtil.wrapWithColorTag("medium", ClueTier.MEDIUM.getColor(transparent)) + separator;
			if (!hasHard) message += ColorUtil.wrapWithColorTag("hard", ClueTier.HARD.getColor(transparent)) + separator;
			if (!hasElite) message += ColorUtil.wrapWithColorTag("elite", ClueTier.ELITE.getColor(transparent)) + separator;
			return message;
		}
	}
}
