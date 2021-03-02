package de.msz.games.games.herorealms;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RestController
@RequestMapping("/hero_realms")
public class HeroRealmsController {
	
	@Autowired
	private HeroRealmsTableService tableService;
	
	@Autowired
	private HeroRealmsActionsService actionsService;
	
	@PostMapping("/{gameId}/play_card")
	public void playCard(@PathVariable("gameId") String gameId, @RequestBody PlayCardRequest playCardRequest)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.playCard(table, playCardRequest.getCardId());
		tableService.storeTable(gameId, table);
	}
	
	@PostMapping("/{gameId}/end_turn")
	public void endTurn(@PathVariable("gameId") String gameId) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.endTurn(table);
		tableService.storeTable(gameId, table);
	}
	
	@Data
	public static class PlayCardRequest {
		private String cardId;
	}
}
