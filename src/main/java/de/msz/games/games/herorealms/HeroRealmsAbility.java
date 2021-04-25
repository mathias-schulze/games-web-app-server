package de.msz.games.games.herorealms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeroRealmsAbility {

	private HeroRealmsAbilityType type;
	@Builder.Default
	private int value = 1;
}
