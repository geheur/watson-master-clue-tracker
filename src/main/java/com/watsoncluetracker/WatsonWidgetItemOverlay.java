package com.watsoncluetracker;

import com.google.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import java.awt.*;

public class WatsonWidgetItemOverlay extends WidgetItemOverlay
{
    @Inject
    private ItemManager itemManager;

    private final WatsonPlugin plugin;
    private final WatsonConfig config;

    @Inject
    WatsonWidgetItemOverlay(WatsonPlugin watsonPlugin, WatsonConfig config)
    {
        this.plugin = watsonPlugin;
        this.config = config;
        showOnInventory();
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if(!config.showClueScrollItemOverlay())
        {
            return;
        }

        int baseItemId = ItemVariationMapping.map(itemId);
        if(baseItemId != WatsonPlugin.CLUE_SCROLL_ITEM_BASE_ID)
        {
            return;
        }

        String itemName = itemManager.getItemComposition(itemId).getName();
        ClueTier clueTier = ClueTier.getClueTier(itemName);
        if(clueTier == null)
        {
            return;
        }

        if(plugin.watsonHasClue(clueTier)) {
            graphics.setFont(FontManager.getRunescapeSmallFont());
            final Rectangle bounds = widgetItem.getCanvasBounds();
            final TextComponent textComponent = new TextComponent();
            textComponent.setPosition(new Point(bounds.x, bounds.y + (int)bounds.getHeight()));
            textComponent.setText("W");
            textComponent.setColor(new Color(0x59a8eb));
            textComponent.render(graphics);
        }
    }
}
