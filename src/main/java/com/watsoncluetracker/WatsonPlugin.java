package com.watsoncluetracker;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.watsoncluetracker.NpcDialogTracker.NpcDialogState;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MessageNode;
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

    enum ClueTier
    {
        EASY("easyClueStored", 0x32a836),
        MEDIUM("mediumClueStored", 0x3fcc8f),
        HARD("hardClueStored", 0xa641ba),
        ELITE("eliteClueStored", 0xc4b847)
        ;

        private final String configKey;
        private final Color color;

        ClueTier(String configKey, int color) {
            this.configKey = configKey;
            this.color = new Color(color);
        }

        public String getConfigKey() {
            return configKey;
        }

        public Color getColor()
        {
            return color;
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
        if (state.type == NpcDialogState.NpcDialogType.NPC && state.text != null && state.text.startsWith("Nice work") && state.text.endsWith(", I've had one of each lower tier<br>clue scroll from you."))
        {
            setWatsonHasClue(ClueTier.EASY, false);
            setWatsonHasClue(ClueTier.MEDIUM, false);
            setWatsonHasClue(ClueTier.HARD, false);
            setWatsonHasClue(ClueTier.ELITE, false);
            log.debug("Watson gave you a master clue.");
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

        if (checkForCluesHandedToWatson) {
            checkForCluesHandedToWatson = false;

            lastInventory.removeAll(newInventory); // leaves items that were removed.
            for (Integer itemId : lastInventory)
            {
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
            String message = "Watson needs:" + separator;
            if (!hasEasy) message += ColorUtil.wrapWithColorTag("easy", ClueTier.EASY.getColor()) + separator;
            if (!hasMedium) message += ColorUtil.wrapWithColorTag("medium", ClueTier.MEDIUM.getColor()) + separator;
            if (!hasHard) message += ColorUtil.wrapWithColorTag("hard", ClueTier.HARD.getColor()) + separator;
            if (!hasElite) message += ColorUtil.wrapWithColorTag("elite", ClueTier.ELITE.getColor()) + separator;
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
