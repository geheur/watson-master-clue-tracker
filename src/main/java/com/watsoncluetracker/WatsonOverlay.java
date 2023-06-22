package com.watsoncluetracker;

import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
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

		final int widgetGroupId = WidgetInfo.TO_GROUP(widget.getId());
		int itemId = -1;

		if(widget.getId() == WidgetInfo.INVENTORY.getId()
				|| widgetGroupId == WidgetInfo.EQUIPMENT_INVENTORY_ITEMS_CONTAINER.getGroupId()
				|| widget.getId() == WidgetInfo.BANK_ITEM_CONTAINER.getId()
				|| widgetGroupId == WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getGroupId())
		{
			itemId = widget.getItemId();
		}
		if(itemId == -1)
		{
			return null;
		}

		int baseItemId = ItemVariationMapping.map(itemId);
		if(baseItemId != WatsonPlugin.CLUE_SCROLL_ITEM_BASE_ID)
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
