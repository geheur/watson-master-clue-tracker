package com.watsoncluetracker;

import com.google.inject.Inject;
import com.watsoncluetracker.WatsonConfig.ShowItemOverlay;
import static com.watsoncluetracker.WatsonConfig.ShowItemOverlay.ALL_TIERS;
import static com.watsoncluetracker.WatsonConfig.ShowItemOverlay.NEVER;
import static com.watsoncluetracker.WatsonConfig.ShowItemOverlay.WATSON_HAS_CLUE;
import static com.watsoncluetracker.WatsonConfig.ShowItemOverlay.WATSON_NEEDS_CLUE;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
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
		ShowItemOverlay showItemOverlay = config.whenToShowItemOverlay();
		if (showItemOverlay == NEVER) return;

		ClueTier clueTier = ClueTier.getClueTier(itemManager.getItemComposition(itemId).getMembersName());
		if(clueTier == null || clueTier == ClueTier.MASTER)
        {
            return;
        }

		boolean watsonHasClue = plugin.watsonHasClue(clueTier);
		if (
			(watsonHasClue && showItemOverlay == WATSON_NEEDS_CLUE) ||
			(!watsonHasClue && showItemOverlay == WATSON_HAS_CLUE)
		) {
			return;
		}

		final Rectangle bounds = widgetItem.getCanvasBounds();
		if (showItemOverlay == ALL_TIERS) {
			int x = bounds.x + 3;
			for (ClueTier tier : ClueTier.values())
			{
				if (tier == ClueTier.MASTER) continue;
				graphics.setColor(tier.getColor(true));
				if (plugin.watsonHasClue(tier))
				{
					graphics.fillRect(x, bounds.y + 24, 5, 5);
				} else {
					graphics.drawRect(x, bounds.y + 24, 4, 4);
				}
				x += 7;
			}
		} else {
			graphics.setFont(FontManager.getRunescapeSmallFont());
			final TextComponent textComponent = new TextComponent();
			textComponent.setPosition(new Point(bounds.x, bounds.y + (int) bounds.getHeight()));
			textComponent.setText("w");
			textComponent.setColor(watsonHasClue ? config.watsonHasClueColor() : config.watsonNeedsClueColor());
			textComponent.render(graphics);
		}
	}
}
