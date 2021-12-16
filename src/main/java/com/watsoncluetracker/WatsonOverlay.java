package com.watsoncluetracker;

import com.watsoncluetracker.WatsonPlugin.ClueTier;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
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
        if (!config.showClueScrollTooltip()) return null;

        MenuEntry[] menu = client.getMenuEntries();
        MenuEntry entry = menu[menu.length - 1];
        int widgetGroupId = WidgetInfo.TO_GROUP(entry.getParam1());
        if (widgetGroupId == 0) return null;

        int itemId = entry.getIdentifier();

        int baseItemId = ItemVariationMapping.map(itemId);
        if (baseItemId != 713) return null;

        String itemName = itemManager.getItemComposition(itemId).getName();
        ClueTier clueTier = ClueTier.getClueTier(itemName);
        if (clueTier == null && itemId != ItemID.CLUE_SCROLL_MASTER) return null;

        tooltipManager.add(new Tooltip(plugin.generateWatsonNeedsText("</br>")));

        return null;
    }

}
