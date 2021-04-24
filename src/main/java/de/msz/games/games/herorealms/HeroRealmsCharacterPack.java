package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public enum HeroRealmsCharacterPack {
	
	CLERIC(55, "extern/hero_realms/CCL-EN-002-cleric-bless.jpg", "extern/hero_realms/CCL-EN-011-cleric-resurrect.jpg"),
	FIGHTER(60, "extern/hero_realms/CFT-EN-010-fighter-shoulder-bash.jpg", "extern/hero_realms/CFT-EN-004-fighter-crushing-blow.jpg"),
	RANGER(58, "extern/hero_realms/CRA-EN-011-ranger-track.jpg", "extern/hero_realms/CRA-EN-006-ranger-headshot.jpg"),
	THIEF(52, "extern/hero_realms/CTH-EN-009-thief-pick-pocket.jpg", "extern/hero_realms/CTH-EN-012-thief-heist.jpg"),
	WIZARD(50, "extern/hero_realms/CWZ-EN-009-wizard-channel.jpg", "extern/hero_realms/CWZ-EN-011-wizard-fireball.jpg"),
	;
	
	private final int initialHealth;
	private final String roundAbilityImage;
	private final String oneTimeAbilityImage;
	
	private HeroRealmsCharacterPack(int initialHealth, String roundAbilityImage, String oneTimeAbilityImage) {
		this.initialHealth = initialHealth;
		this.roundAbilityImage = roundAbilityImage;
		this.oneTimeAbilityImage = oneTimeAbilityImage;
	}
	
	public static List<HeroRealmsCharacterPack> getRandomList() {
		
		List<HeroRealmsCharacterPack> packs = new ArrayList<>(Arrays.asList(values()));
		Collections.shuffle(packs);
		return packs;
	}
}
