package com.watsoncluetracker;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.watsoncluetracker.NpcDialogTracker.NpcDialogState;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MessageNode;
import net.runelite.api.Varbits;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.util.Text;

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
    private ConfigManager configManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WatsonConfig watsonConfig;

    @Inject
    private WatsonOverlay watsonOverlay;

    @Inject
    private WatsonWidgetItemOverlay watsonWidgetItemOverlay;

    @Inject
    private EventBus eventBus;

    @Inject
    private NpcDialogTracker npcDialogTracker;

    // item ids in inventory. Doesn't store order or quantity.
    private Collection<Integer> lastInventory = Collections.emptyList();
    private boolean checkForCluesHandedToWatson = false;
    /** checking the tick number is needed here because it's possible for no inventory update to happen, and that does not generate an event in which we can reset this variable like we can for checkForCluesHandedToWatson. */
	private int checkForMasterClueHandedToPlayer = -1;

	enum ClueTier
    {
        EASY("easyClueStored", new Color(0x32a836), new Color(0x32a836).darker()),
        MEDIUM("mediumClueStored", new Color(0x3fcc8f), new Color(0x3fcc8f).darker()),
        HARD("hardClueStored", new Color(0xa641ba), new Color(0xa641ba)),
        ELITE("eliteClueStored", new Color(0xc4b847), new Color(0xc4b847).darker())
        ;

        private final String configKey;
        private final Color colorTransparent;
        private final Color colorOpaque;

        ClueTier(String configKey, Color colorTransparent, Color colorOpaque) {
            this.configKey = configKey;
            this.colorTransparent = colorTransparent;
            this.colorOpaque = colorOpaque;
        }

        public String getConfigKey() {
            return configKey;
        }

        public Color getColor(boolean transparentChatbox)
        {
            return transparentChatbox ? colorTransparent : colorOpaque;
        }

        public static ClueTier getClueTier(String clueName)
        {
            return clueName.equals("Clue scroll (easy)") ? ClueTier.EASY :
                   clueName.equals("Clue scroll (medium)") ? ClueTier.MEDIUM :
                   clueName.equals("Clue scroll (hard)") ? ClueTier.HARD :
                   clueName.equals("Clue scroll (elite)") ? ClueTier.ELITE :
                   null
                   ;
        }
    }

    public boolean watsonHasClue(ClueTier clueTier) {
        return Boolean.parseBoolean(configManager.getRSProfileConfiguration(CONFIG_KEY, clueTier.getConfigKey()));
    }

    public void setWatsonHasClue(ClueTier clueTier, boolean watsonHasClue) {
        configManager.setRSProfileConfiguration(CONFIG_KEY, clueTier.getConfigKey(), watsonHasClue);
        log.debug("Watson has " + clueTier + " clue: " + watsonHasClue);
    }

    @Provides
    WatsonConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(WatsonConfig.class);
    }

    @Override
    public void startUp() {
        overlayManager.add(watsonOverlay);
        overlayManager.add(watsonWidgetItemOverlay);
        npcDialogTracker.reset();
        eventBus.register(npcDialogTracker);
        npcDialogTracker.setStateChangedListener(this::npcDialogStateChanged);
        npcDialogTracker.setOptionSelectedListener(this::optionSelected);
    }

    @Override
    public void shutDown() {
        overlayManager.remove(watsonOverlay);
        overlayManager.remove(watsonWidgetItemOverlay);
        eventBus.unregister(npcDialogTracker);
    }

    void npcDialogStateChanged(NpcDialogState state) {
        if (state.type == NpcDialogState.NpcDialogType.NPC) {
            checkWatsonCluesNeededDialog(state);
        }
    }

    void optionSelected(NpcDialogState state, String option) {
        String text = Text.sanitizeMultilineText(state.text);
        if (state.type == NpcDialogState.NpcDialogType.NPC && text != null && text.startsWith("Nice work") && text.endsWith(", I've had one of each lower tier clue scroll from you."))
        {
        	checkForMasterClueHandedToPlayer = client.getTickCount();
        } else if (state.type == NpcDialogState.NpcDialogType.OPTIONS && option != null && option.startsWith("Hand over ")) {
            // Options can be "Hand over hard clue." or "Hand over all.".
            checkForCluesHandedToWatson = true;
        }
    }

    private void checkWatsonCluesNeededDialog(NpcDialogState state)
    {
        if (state.name == null || state.text == null) return;

        if ("Watson".equals(state.name) && (state.text.startsWith("I still need") || state.text.startsWith("Nice work ")))
        {
            boolean easyClue = state.text.indexOf("easy") == -1;
            boolean mediumClue = state.text.indexOf("medium") == -1;
            boolean hardClue = state.text.indexOf("hard") == -1;
            boolean eliteClue = state.text.indexOf("elite") == -1;
            setWatsonHasClue(ClueTier.EASY, easyClue);
            setWatsonHasClue(ClueTier.MEDIUM, mediumClue);
            setWatsonHasClue(ClueTier.HARD, hardClue);
            setWatsonHasClue(ClueTier.ELITE, eliteClue);
        }
    }

    // This fires on login unless your inventory is empty (which is fine for my purposes).
    // This means that it's not possible to log in next to watson with a clue in the inventory already without
    // this plugin knowing which clues you have, since that would prevent detection of which clues were handed over.
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged itemContainerChanged) {
        ItemContainer itemContainer = itemContainerChanged.getItemContainer();
        if (itemContainerChanged.getContainerId() != InventoryID.INVENTORY.getId()) return;

        List<Integer> newInventory = Arrays.stream(itemContainer.getItems()).map(item -> item.getId()).collect(Collectors.toList());
        if (newInventory.equals(lastInventory)) return;

        log.debug("inventory changed: {} -> {} (game tick: {})", lastInventory, newInventory, client.getTickCount());

        if (client.getTickCount() == checkForMasterClueHandedToPlayer || client.getTickCount() == checkForMasterClueHandedToPlayer + 1) { // can be either same tick or next tick depending on timing or something idk exactly.
			List<Integer> newInventoryCopy = new ArrayList<>(newInventory);
			newInventoryCopy.removeAll(lastInventory); // leaves items that were added.
			for (Integer itemId : newInventoryCopy)
			{
				if (itemId == -1) continue;
				String name = itemManager.getItemComposition(itemId).getName();
				if ("Clue scroll (master)".equals(name)) {
					setWatsonHasClue(ClueTier.EASY, false);
					setWatsonHasClue(ClueTier.MEDIUM, false);
					setWatsonHasClue(ClueTier.HARD, false);
					setWatsonHasClue(ClueTier.ELITE, false);
					log.debug("Watson gave you a master clue.");
				} else {
					log.debug("Watson did not give you a master clue.");
				}
			}
		}

        if (checkForCluesHandedToWatson) {
            checkForCluesHandedToWatson = false;

            lastInventory.removeAll(newInventory); // leaves items that were removed.
            for (Integer itemId : lastInventory)
            {
				if (itemId == -1) continue;
                String name = itemManager.getItemComposition(itemId).getName();
                ClueTier clueTier = ClueTier.getClueTier(name);
                if (clueTier != null)
                {
                    setWatsonHasClue(clueTier, true);
                }
            }
        }

        lastInventory = newInventory;
    }

    public String generateWatsonNeedsText(String separator) {
        boolean hasEasy = watsonHasClue(ClueTier.EASY);
        boolean hasMedium = watsonHasClue(ClueTier.MEDIUM);
        boolean hasHard = watsonHasClue(ClueTier.HARD);
        boolean hasElite = watsonHasClue(ClueTier.ELITE);
        if (hasEasy && hasMedium && hasHard && hasElite) {
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

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        if (watsonConfig.watsonChatCommand() && "watson".equals(commandExecuted.getCommand()))
        {
            chatMessage(generateWatsonNeedsText(" "));
        }
    }

    private MessageNode chatMessage(String message) {
        return client.addChatMessage(ChatMessageType.GAMEMESSAGE, "bla", message, "bla");
    }

}
