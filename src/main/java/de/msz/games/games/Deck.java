package de.msz.games.games;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class Deck<T extends Card> {
	
	protected int size;
	
	private LinkedList<T> cards = new LinkedList<>();
	
	public T draw() {
		
		if (!cards.isEmpty()) {
			size--;
			return cards.removeFirst();
		}
		
		return null;
	}
	
	public final void setCards(List<T> cards) {
		setCards(cards, false);
	}
	
	public final void setCards(List<T> cards, boolean shuffle) {
		
		if (shuffle) {
			Collections.shuffle(cards);
		}
		
		this.cards = new LinkedList<>(cards);
		size = cards.size();
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Card> Deck<T> from(Map<String, Object> map, Class<T> clazz) {
		
		int size = Long.valueOf((long) map.get("size")).intValue();
		
		List<T> cards = ((List<Map<String, Object>>) map.get("cards")).stream()
				.map(card -> Card.from(card, clazz))
				.collect(Collectors.toList());
		
		Deck<T> deck = new Deck<>();
		deck.setCards(cards);
		deck.setSize(size);
		
		return deck;
	}
}
