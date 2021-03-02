package de.msz.games.games.herorealms;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HeroRealmsCardAbilities {
	
	private HeroRealmsAbilitySet primaryAbility;
	private HeroRealmsAbilitySet allyAbility;
	private HeroRealmsAbilitySet sacrificeAbility;
}
