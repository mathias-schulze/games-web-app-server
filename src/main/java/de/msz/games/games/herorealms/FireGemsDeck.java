package de.msz.games.games.herorealms;

import java.util.Arrays;
import java.util.Map;

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
	
	public static FireGemsDeck fromSub(Map<String, Object> map) {
		
		Deck<HeroRealmsCard> deck = Deck.from(map, HeroRealmsCard.class);
		return (new FireGemsDeck(deck.getSize(), deck.getCards().get(0)));
	}
}
