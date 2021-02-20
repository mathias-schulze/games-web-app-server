package de.msz.games.games;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
}
