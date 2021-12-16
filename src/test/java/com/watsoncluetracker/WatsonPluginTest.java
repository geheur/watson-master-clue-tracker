package com.watsoncluetracker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import com.watsoncluetracker.NpcDialogTracker.NpcDialogState;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WatsonPluginTest
{
    @Mock
    @Bind
    private Client client;

    @Mock
    @Bind
    private ConfigManager configManager;

    @Mock
    @Bind
    private ItemManager itemManager;

    @Mock
    @Bind
    private OverlayManager overlayManager;

    @Mock
    @Bind
    private WatsonConfig watsonConfig;

    @Mock
    @Bind
    private WatsonOverlay watsonOverlay;

    @Mock
    @Bind
    private WatsonWidgetItemOverlay watsonWidgetItemOverlay;

    @Mock
    @Bind
    private NpcDialogTracker npcDialogTracker;

    @Mock
    @Bind
    private MouseManager mouseManager;

    @Inject
    private WatsonPlugin plugin;

    private Map<String, Object> configManagerMock = new HashMap<>();
	private int gameTick = 0;

	@Before
    public void before() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);

        Player mockPlayer = mock(Player.class);
        // This is the worst name I could think of.
        when(mockPlayer.getName()).thenReturn("bla" + " " + "183" + "&nbsp;" + "BLA");
        when(client.getLocalPlayer()).thenReturn(mockPlayer);
		when(client.getTickCount()).thenAnswer(invocation -> gameTick);

		final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.DEBUG);

        when(itemManager.getItemComposition(anyInt())).thenAnswer(invocation ->
        {
            ItemComposition itemComposition = mock(ItemComposition.class);
            if ((Integer) invocation.getArgument(0) == 2677)
            {
                when(itemComposition.getName()).thenReturn("Clue scroll (easy)");
            } else if ((Integer) invocation.getArgument(0) == 2801)
            {
                when(itemComposition.getName()).thenReturn("Clue scroll (medium)");
            } else if ((Integer) invocation.getArgument(0) == 19858)
            {
                when(itemComposition.getName()).thenReturn("Clue scroll (hard)");
            } else if ((Integer) invocation.getArgument(0) == 19835) {
                when(itemComposition.getName()).thenReturn("Clue scroll (master)");
            } else {
                when(itemComposition.getName()).thenReturn("some other item name");
            }
            return itemComposition;
        });

        Mockito.when(configManager.getRSProfileConfiguration(Mockito.eq(WatsonPlugin.CONFIG_KEY), Mockito.anyString()))
                .thenAnswer(invocation -> configManagerMock.get(invocation.getArgument(1)));

        Mockito.doAnswer((Answer<Void>) invocation -> {
            configManagerMock.put(invocation.getArgument(1), invocation.getArgument(2).toString());
            return null;
        }).when(configManager).setRSProfileConfiguration(Mockito.eq(WatsonPlugin.CONFIG_KEY), Mockito.anyString(), Mockito.any(boolean.class));
    }

    @Test
    public void testCheckHardEliteClue() {
        plugin.npcDialogStateChanged(NpcDialogState.noDialog());
        setClues(false, false, true, true);

        plugin.npcDialogStateChanged(NpcDialogState.npc("Watson", "I still need a hard clue and an elite clue from you<br>before I give you a master clue scroll."));

        testClues(true, true, false, false);
    }

    private void setClues(boolean easy, boolean medium, boolean hard, boolean elite)
    {
        plugin.setWatsonHasClue(WatsonPlugin.ClueTier.EASY, easy);
        plugin.setWatsonHasClue(WatsonPlugin.ClueTier.MEDIUM, medium);
        plugin.setWatsonHasClue(WatsonPlugin.ClueTier.HARD, hard);
        plugin.setWatsonHasClue(WatsonPlugin.ClueTier.ELITE, elite);
    }

    private void testClues(boolean easy, boolean medium, boolean hard, boolean elite)
    {
        assertEquals("wrong easy clue state", easy, plugin.watsonHasClue(WatsonPlugin.ClueTier.EASY));
        assertEquals("wrong medium clue state", medium, plugin.watsonHasClue(WatsonPlugin.ClueTier.MEDIUM));
        assertEquals("wrong hard clue state", hard, plugin.watsonHasClue(WatsonPlugin.ClueTier.HARD));
        assertEquals("wrong elite clue state", elite, plugin.watsonHasClue(WatsonPlugin.ClueTier.ELITE));
    }

    private void printClues() {
        System.out.println("watson has: " + plugin.watsonHasClue(WatsonPlugin.ClueTier.EASY) + " " + plugin.watsonHasClue(WatsonPlugin.ClueTier.MEDIUM) + " " + plugin.watsonHasClue(WatsonPlugin.ClueTier.HARD) + " " + plugin.watsonHasClue(WatsonPlugin.ClueTier.ELITE));
    }

    @Test
    public void testHandOverHardClue() {
        plugin.npcDialogStateChanged(NpcDialogState.noDialog());
        setClues(true, true, false, false);
        inventoryChange(19858);

        NpcDialogState select_an_option = NpcDialogState.options("Select an Option", "Hand over hard clue.", "Cancel.");
        plugin.npcDialogStateChanged(select_an_option);
        plugin.optionSelected(select_an_option, "Hand over hard clue.");

        inventoryChange(-1);

        testClues(true, true, true, false);
    }

    @Test
    public void testHandOverMultipleClues() {
        plugin.npcDialogStateChanged(NpcDialogState.noDialog());
        setClues(false, false, true, true);
        inventoryChange(2677, 2801);

        NpcDialogState select_an_option = NpcDialogState.options("Select an Option", "Hand over easy clue.", "Hand over medium clue.", "Hand over all.", "Cancel.");
        plugin.npcDialogStateChanged(select_an_option);
        plugin.optionSelected(select_an_option, "Hand over all.");

        inventoryChange();

        testClues(true, true, true, true);
    }

    @Test
    public void testWatsonCheckDialogue() {
        plugin.npcDialogStateChanged(NpcDialogState.npc("Watson", "I still need an easy clue, a medium clue and an elite<br>clue from you before I give you a master clue scroll."));
        testClues(false, false, true, false);
    }

    @Test
    public void testReceiveMasterClue() {
        plugin.npcDialogStateChanged(NpcDialogState.noDialog());
        setClues(true, true, false, false);
        inventoryChange(-1);

        NpcDialogState state = NpcDialogState.npc("Watson", "Nice work " + client.getLocalPlayer().getName() + ", I've had one of each lower tier<br>clue scroll from you.");
        plugin.npcDialogStateChanged(state);
        plugin.optionSelected(state, null);

        gameTick++;

        inventoryChange(-1, 19835);

        testClues(false, false, false, false);
    }

	@Test
	public void testUnableToReceiveMasterClue() {
		plugin.npcDialogStateChanged(NpcDialogState.noDialog());
		setClues(true, true, false, false);
		inventoryChange(-1);

		NpcDialogState state = NpcDialogState.npc("Watson", "Nice work " + client.getLocalPlayer().getName() + ", I've had one of each lower tier<br>clue scroll from you.");
		plugin.npcDialogStateChanged(state);
		plugin.optionSelected(state, null);

		gameTick++;

//		inventoryChange(-1, 19835);

		testClues(true, true, true, true);
	}

	private ItemContainer container = Mockito.mock(ItemContainer.class);

    private void inventoryChange(int... itemIds)
    {
        Item items[] = new Item[itemIds.length];
        for (int i = 0; i < itemIds.length; i++)
        {
            items[i] = new Item(itemIds[i], 1);
        }
        Mockito.when(container.getItems()).thenReturn(items);
        plugin.onItemContainerChanged(new ItemContainerChanged(93, container));
    }
}
