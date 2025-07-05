package com.watsoncluetracker;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;

@AllArgsConstructor
@Getter
public enum ClueTier
{
	EASY(new Color(0x32a836), new Color(0x32a836).darker()),
	MEDIUM(new Color(0x3fcc8f), new Color(0x3fcc8f).darker()),
	HARD(new Color(0xa641ba), new Color(0xa641ba)),
	ELITE(new Color(0xc4b847), new Color(0xc4b847).darker()),
	MASTER(null, null);

	private final Color colorTransparent;
	private final Color colorOpaque;

	/**
	 * Returns the tier for non-beginner clue scrolls, geodes, bottles, nests, and scroll boxes.
	 */
	public static ClueTier getClueTier(String itemName)
	{
		if (itemName.endsWith(")") && (itemName.startsWith("Clue") || itemName.startsWith("Scroll box"))) {
			return
				itemName.endsWith("(easy)") ? ClueTier.EASY :
				itemName.endsWith("(medium)") ? ClueTier.MEDIUM :
				itemName.endsWith("(hard)") ? ClueTier.HARD :
				itemName.endsWith("(elite)") ? ClueTier.ELITE :
				itemName.endsWith("(master)") ? ClueTier.MASTER :
				null
			;
		} else {
			return null;
		}
	}

	public Color getColor(boolean transparentChatbox)
	{
		return transparentChatbox ? colorTransparent : colorOpaque;
	}

}
