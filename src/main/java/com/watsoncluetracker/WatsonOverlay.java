package com.watsoncluetracker;

import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;

public class WatsonOverlay extends Overlay
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private WatsonConfig config;

	@Inject
	private WatsonPlugin plugin;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if(!config.showClueScrollTooltip())
		{
			return null;
		}

		final MenuEntry[] menu = client.getMenuEntries();
		final int menuSize = menu.length;
		if(menuSize <= 0)
		{
			return null;
		}

		final MenuEntry entry = menu[menuSize - 1];
		final Widget widget = entry.getWidget();
		if(widget == null)
		{
			return null;
		}

		int itemId = -1;

		final int widgetGroupId = WidgetUtil.componentToInterface(widget.getId());
		if(widgetGroupId == InterfaceID.INVENTORY
				|| widgetGroupId == InterfaceID.EQUIPMENT
				|| widgetGroupId == InterfaceID.BANK
				|| widgetGroupId == InterfaceID.BANK_INVENTORY)
		{
			itemId = widget.getItemId();
		}
		if(itemId == -1)
		{
			return null;
		}

		int baseItemId = ItemVariationMapping.map(itemId);
		if(!WatsonPlugin.CLUE_SCROLL_BASE_IDS.contains(baseItemId))
		{
			return null;
		}

		String itemName = itemManager.getItemComposition(itemId).getMembersName();
		ClueTier clueTier = ClueTier.getClueTier(itemName);
		if(clueTier == null && itemId != ItemID.CLUE_SCROLL_MASTER)
		{
			return null;
		}

		tooltipManager.add(new Tooltip(plugin.generateWatsonNeedsText("</br>")));

		return null;
	}
}
