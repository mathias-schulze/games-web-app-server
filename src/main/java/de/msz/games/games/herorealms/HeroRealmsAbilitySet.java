package de.msz.games.games.herorealms;

import java.util.List;

import lombok.Data;

@Data
public class HeroRealmsAbilitySet {
	
	private HeroRealmsAbilityLinkage linkage = HeroRealmsAbilityLinkage.AND;
	private List<HeroRealmsAbility> abilities;
}
