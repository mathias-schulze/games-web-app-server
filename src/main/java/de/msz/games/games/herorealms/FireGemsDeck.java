package de.msz.games.games.herorealms;

import java.util.Arrays;

import de.msz.games.games.Deck;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper=false)
@Setter(value = AccessLevel.PROTECTED)
public class FireGemsDeck extends Deck<HeroRealmsCard> {
	
	public FireGemsDeck(int initialCount, HeroRealmsCard fireGem) {
		setCards(Arrays.asList(fireGem));
		this.size = initialCount;
	}
	
	@Override
	public HeroRealmsCard draw() {
		
		if (size == 0) {
			return null;
		}
		
		size--;
		return getCards().get(0);
	}
}
