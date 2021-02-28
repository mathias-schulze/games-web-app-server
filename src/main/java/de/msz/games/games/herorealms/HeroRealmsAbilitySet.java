package de.msz.games.games.herorealms;

import java.util.List;

import de.msz.games.games.herorealms.config.HeroRealmsJsonAbility;
import lombok.Data;

@Data
public class HeroRealmsAbilitySet {
	
	private HeroRealmsAbilityLinkage linkage = HeroRealmsAbilityLinkage.AND;
	private List<HeroRealmsJsonAbility> abilities;
}
