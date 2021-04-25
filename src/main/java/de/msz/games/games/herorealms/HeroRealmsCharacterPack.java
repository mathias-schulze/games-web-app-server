package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public enum HeroRealmsCharacterPack {
	
	CLERIC(55,
		"extern/hero_realms/CCL-EN-002-cleric-bless.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.CLERIC_BLESS).build()
		},
		"extern/hero_realms/CCL-EN-011-cleric-resurrect.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.PUT_CHAMPION_DISCARD_INTO_PLAY).build()
		}),
	FIGHTER(60,
		"extern/hero_realms/CFT-EN-010-fighter-shoulder-bash.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.COMBAT).value(2).build()
		},
		"extern/hero_realms/CFT-EN-004-fighter-crushing-blow.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.COMBAT).value(8).build()
		}),
	RANGER(58,
		"extern/hero_realms/CRA-EN-011-ranger-track.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.RANGER_TRACK).build()
		},
		"extern/hero_realms/CRA-EN-006-ranger-headshot.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.DRAW_CARD).build(),
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.STUN_TARGET_CHAMPION).build()
		}),
	THIEF(52,
		"extern/hero_realms/CTH-EN-009-thief-pick-pocket.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.HEALTH).value(3).build(),
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.OPPONENT_DISCARD_CARD).build()
		},
		"extern/hero_realms/CTH-EN-012-thief-heist.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.ACQUIRE_OPPONENT_DISCARD).build()
		}),
	WIZARD(50,
		"extern/hero_realms/CWZ-EN-009-wizard-channel.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.HEALTH).value(-1).build(),
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.DRAW_CARD).build()
		},
		"extern/hero_realms/CWZ-EN-011-wizard-fireball.jpg",
		new HeroRealmsAbility[] {
				HeroRealmsAbility.builder().type(HeroRealmsAbilityType.WIZARD_FIREBALL).build()
		}),
	;
	
	private final int initialHealth;
	private final String roundAbilityImage;
	private final HeroRealmsAbility[] roundAbilities;
	private final String oneTimeAbilityImage;
	private final HeroRealmsAbility[] oneTimeAbilities;
	
	private HeroRealmsCharacterPack(int initialHealth, String roundAbilityImage, HeroRealmsAbility[] roundAbilities,
			String oneTimeAbilityImage, HeroRealmsAbility[] oneTimeAbilities) {
		this.initialHealth = initialHealth;
		this.roundAbilityImage = roundAbilityImage;
		this.roundAbilities = roundAbilities;
		this.oneTimeAbilityImage = oneTimeAbilityImage;
		this.oneTimeAbilities = oneTimeAbilities;
	}
	
	public static List<HeroRealmsCharacterPack> getRandomList() {
		
		List<HeroRealmsCharacterPack> packs = new ArrayList<>(Arrays.asList(values()));
		Collections.shuffle(packs);
		return packs;
	}
}
