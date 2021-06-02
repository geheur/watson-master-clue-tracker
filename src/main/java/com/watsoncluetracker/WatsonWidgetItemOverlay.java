package com.watsoncluetracker;

import com.google.inject.Inject;
import com.watsoncluetracker.WatsonPlugin.ClueTier;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import java.awt.*;

public class WatsonWidgetItemOverlay extends WidgetItemOverlay
{
    @Inject
    private Client client;

    @Inject
    private WatsonConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private WatsonPlugin plugin;

    {
        showOnInventory();
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!config.showClueScrollItemOverlay()) return;

        int baseItemId = ItemVariationMapping.map(itemId);
        if (baseItemId != 713) return;

        String clueName = itemManager.getItemComposition(itemId).getName();
        ClueTier clueTier = ClueTier.getClueTier(clueName);
        if (clueTier == null) return;

        Rectangle bounds = widgetItem.getCanvasBounds();
        graphics.setColor(plugin.watsonHasClue(clueTier) ? Color.GREEN : Color.RED);
        graphics.drawString("Watson", (int) bounds.getX(), (int) (bounds.getY() + bounds.getHeight()));
    }
}
