package com.watsoncluetracker;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Color;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum ClueTier
{
	EASY("Clue scroll (easy)", new Color(0x32a836), new Color(0x32a836).darker()),
	MEDIUM("Clue scroll (medium)", new Color(0x3fcc8f), new Color(0x3fcc8f).darker()),
	HARD("Clue scroll (hard)", new Color(0xa641ba), new Color(0xa641ba)),
	ELITE("Clue scroll (elite)", new Color(0xc4b847), new Color(0xc4b847).darker());

	private final String clueName;
	private final Color colorTransparent;
	private final Color colorOpaque;

	private static final Map<String, ClueTier> map;
	static {
		map = Arrays.stream(values())
				.collect(Collectors.toMap(e -> e.clueName, e -> e));
	}

	public static ClueTier getClueTier(String name)
	{
		return map.get(name);
	}

	public Color getColor(boolean transparentChatbox)
	{
		return transparentChatbox ? colorTransparent : colorOpaque;
	}

}
