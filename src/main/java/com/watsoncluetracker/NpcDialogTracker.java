package com.watsoncluetracker;

import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Things to test: clicks and keystrokes for click here to continue in NPC, PLAYER, SPRITE, and NPC DIALOG OPTIONS
// (all 5 options!).

/**
 * need to register it to the eventbus and mousemanager for it to work.
 *
 * Not working: meslayer (I'm assuming, cause I don't know what meslayer actually is), sprite (partial).
 */
@Slf4j
public class NpcDialogTracker
{
    private static final int WIDGET_CHILD_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE = 5;
    private static final int WIDGET_CHILD_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE = 5;
    private static final int WIDGET_CHILD_ID_DIALOG_PLAYER_NAME = 4; // For some reason there is no WidgetInfo for this despite there being an (innaccessible to me) widgetid for this in WidgetID.

    @Inject
    private Client client;

    private Consumer<NpcDialogState> npcDialogStateChanged;
    private BiConsumer<NpcDialogState, String> npcDialogOptionSelected;

    private NpcDialogState lastNpcDialogState = null;

    public void setStateChangedListener(Consumer<NpcDialogState> listener) {
        npcDialogStateChanged = listener;
    }

    public void setOptionSelectedListener(BiConsumer<NpcDialogState, String> listener) {
        npcDialogOptionSelected = listener;
    }

    /*
    It's possible to miss a click but I've only seen then when I'm moving the mouse as fast as I possibly can.
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        int groupId = WidgetInfo.TO_GROUP(event.getWidgetId());
        int childId = WidgetInfo.TO_CHILD(event.getWidgetId());
        if (event.getWidgetId() == WidgetInfo.DIALOG_OPTION_OPTIONS.getId()) {
            Widget widget = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
            int dynamicChildIndex = event.getActionParam();
            Widget[] dynamicChildren = widget.getDynamicChildren();
            Widget dynamicChild = dynamicChildren[dynamicChildIndex];
            if (dynamicChild == null)
            {
                log.debug("dynamic child option was null, index " + dynamicChildIndex + " total children: " + dynamicChildren.length);
                return; // not sure why this would happen.
            }
            optionSelected(lastNpcDialogState, dynamicChild.getText());
        } else if (groupId == WidgetID.DIALOG_NPC_GROUP_ID && childId == WIDGET_CHILD_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE) {
            optionSelected(lastNpcDialogState, null);
        } else if (groupId == WidgetID.DIALOG_PLAYER_GROUP_ID && childId == WIDGET_CHILD_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE) {
            optionSelected(lastNpcDialogState, null);
        } else if (groupId == WidgetID.DIALOG_SPRITE_GROUP_ID && childId == 0) {
            optionSelected(lastNpcDialogState, null);
        }
    }

    public NpcDialogState getNpcDialogState() {
        NpcDialogState.NpcDialogType type = getNpcDialogType();

        NpcDialogState state;
        switch (type) {
            case NPC:
            {
                Widget nameWidget = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);

                String name = (nameWidget != null) ? nameWidget.getText() : null;
                String text = (textWidget != null) ? textWidget.getText() : null;

                state = NpcDialogState.npc(name, text);
                break;
            }
            case PLAYER:
            {
                Widget nameWidget = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, WIDGET_CHILD_ID_DIALOG_PLAYER_NAME);
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

                String name = (nameWidget != null) ? nameWidget.getText() : null;
                String text = (textWidget != null) ? textWidget.getText() : null;

                state = NpcDialogState.player(name, text);
                break;
            }
            case OPTIONS:
            {
                String text = null;

                Widget optionsWidget = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
                List<String> options = null;
                if (optionsWidget != null) {
                    options = new ArrayList<>();
                    for (Widget child : optionsWidget.getDynamicChildren()) {
                        if (child.getText() != null && !child.getText().isEmpty())
                        {
                            options.add(child.getText());
                        }
                    }
                    text = options.remove(0); // remove "Select an Option".
                }

                state = NpcDialogState.options(text, options);
                break;
            }
            case SPRITE:
            {
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);

                String text = (textWidget != null) ? textWidget.getText() : null;

                state = NpcDialogState.sprite(text);
                break;
            }
            case NO_DIALOG:
            {
                state = NpcDialogState.noDialog();
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        return state;
    }

    private NpcDialogState.NpcDialogType getNpcDialogType()
    {
        Widget npcDialog = client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, 0);
        if (npcDialog != null && !npcDialog.isHidden())
        {
            return NpcDialogState.NpcDialogType.NPC;
        }

        Widget playerDialog = client.getWidget(WidgetInfo.DIALOG_PLAYER);
        if (playerDialog != null && !playerDialog.isHidden())
        {
            return NpcDialogState.NpcDialogType.PLAYER;
        }

        Widget optionsDialog = client.getWidget(WidgetInfo.DIALOG_OPTION);
        if (optionsDialog != null && !optionsDialog.isHidden())
        {
            return NpcDialogState.NpcDialogType.OPTIONS;
        }

        Widget spriteDialog = client.getWidget(WidgetInfo.DIALOG_SPRITE);
        if (spriteDialog != null && !spriteDialog.isHidden())
        {
            return NpcDialogState.NpcDialogType.SPRITE;
        }

        return NpcDialogState.NpcDialogType.NO_DIALOG;
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
//        log.debug("Game tick: {}", client.getTickCount());
        optionSelected = false;

        NpcDialogState npcDialogState = getNpcDialogState();
        if (!Objects.equals(npcDialogState, lastNpcDialogState)) {
            log.debug("{} {} npc dialog changed: {} previous: {} (game tick: {})", client.getTickCount(), System.currentTimeMillis(), npcDialogState, lastNpcDialogState);

            if (npcDialogStateChanged != null) npcDialogStateChanged.accept(npcDialogState);
        }
        lastNpcDialogState = npcDialogState;
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == 2153)
        {
            Widget w = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
            if (w != null && !w.isHidden())
            {
                for (int i = 0; i < w.getDynamicChildren().length; i++)
                {
                    Widget dynamicChild = w.getDynamicChildren()[i];
                    if ("Please wait...".equals(dynamicChild.getText())) {
                        String option = null;
                        if (lastNpcDialogState.type == NpcDialogState.NpcDialogType.OPTIONS) {
                            if (lastNpcDialogState.options != null) {
                                if (lastNpcDialogState.options.size() > i - 1) { // -1 because we skip "Select an Option".
                                    option = lastNpcDialogState.options.get(i - 1); // -1 because we skip "Select an Option".
                                }
                            }
                        }
                        optionSelected(lastNpcDialogState, option);
                    }
                }
            }
            w = client.getWidget(WidgetID.DIALOG_NPC_GROUP_ID, WIDGET_CHILD_ID_DIALOG_NPC_CLICK_HERE_TO_CONTINUE);
            if (w != null && !w.isHidden() && "Please wait...".equals(w.getText()))
            {
                optionSelected(lastNpcDialogState, null);
            }
            w = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, WIDGET_CHILD_ID_DIALOG_PLAYER_CLICK_HERE_TO_CONTINUE);
            if (w != null && !w.isHidden() && "Please wait...".equals(w.getText()))
            {
                optionSelected(lastNpcDialogState, null);
            }
        } else if (event.getScriptId() == 2869) {
            Widget w = client.getWidget(WidgetInfo.DIALOG_SPRITE);
            if (w != null && !w.isHidden())
            {
                Widget dynamicChild = w.getDynamicChildren()[2];
                if ("Please wait...".equals(dynamicChild.getText()))
                {
                    optionSelected(lastNpcDialogState, null);
                }
            }
        }
    }

    /**
     * To prevent multiple selections from occuring in the same game tick. Only the first one should count.
     */
    private boolean optionSelected = false;

    private void optionSelected(NpcDialogState state, String option) {
        if (optionSelected) return;
        optionSelected = true;
        if (state.type == NpcDialogState.NpcDialogType.OPTIONS) {
            log.debug("{} {} option selected: \"{}\" {}", client.getTickCount(), System.currentTimeMillis(), option, state);
        } else {
            log.debug("{} {} clicked here to continue: {}", client.getTickCount(), System.currentTimeMillis(), state);
        }
        if (npcDialogOptionSelected != null) npcDialogOptionSelected.accept(state, option);
    }

    public void reset()
    {
        lastNpcDialogState = null;
    }

    @RequiredArgsConstructor
    public static class NpcDialogState {
        enum NpcDialogType {
            /**
             * NO_DIALOG does NOT indicate the end of a dialog. For example you can end a dialog with an npc by doing something like checking a kharedst memoirs, without seeing the NO_DIALOG state inbetween.
             */
            NO_DIALOG,
            PLAYER,
            NPC,
            OPTIONS,
            SPRITE
        }

        @NonNull
        final NpcDialogType type;

        // Meaningful only when type is PLAYER or NPC.
        @Nullable
        final String name;

        @Nullable
        final String text;

        // Meaningful only when type is OPTIONS
        @Nullable
        final List<String> options;

        public static NpcDialogState sprite(String text) {
            return new NpcDialogState(NpcDialogType.SPRITE, null, text, null);
        }

        public static NpcDialogState player(String name, String text) {
            return new NpcDialogState(NpcDialogType.PLAYER, name, text, null);
        }

        public static NpcDialogState npc(String name, String text) {
            return new NpcDialogState(NpcDialogType.NPC, name, text, null);
        }

        public static NpcDialogState options(String text, List<String> options) {
            return new NpcDialogState(NpcDialogType.OPTIONS, null, text, options);
        }

        public static NpcDialogState options(String text, String... options) {
            return new NpcDialogState(NpcDialogType.OPTIONS, null, text, Arrays.asList(options));
        }

        public static NpcDialogState noDialog() {
            return new NpcDialogState(NpcDialogType.NO_DIALOG, null, null, null);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NpcDialogState that = (NpcDialogState) o;
            return type == that.type && Objects.equals(text, that.text) && Objects.equals(name, that.name) && Objects.equals(options, that.options);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, text, name, options);
        }

        @Override
        public String toString()
        {
            switch (type) {
                case NO_DIALOG:
                    return "NpcDialogState{" + type +
                            "}";
                case PLAYER:
                case NPC:
                    return "NpcDialogState{" + type +
                            ", name='" + name + "'" +
                            ", text='" + text + "'" +
                            "}";
                case SPRITE:
                    return "NpcDialogState{" + type +
                            ", text='" + text + "'" +
                            "}";
                case OPTIONS:
                    return "NpcDialogState{" + type +
                            ", text='" + text + "'" +
                            ", options=" + options +
                            "}";
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
