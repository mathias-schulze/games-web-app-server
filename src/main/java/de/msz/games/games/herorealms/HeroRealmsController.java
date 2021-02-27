package de.msz.games.games.herorealms;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hero_realms")
public class HeroRealmsController {
	
	@Autowired
	private HeroRealmsTableService tableService;
	
	@Autowired
	private HeroRealmsActionsService actionsService;
	
	@PostMapping("/{id}/end_turn")
	public void endTurn(@PathVariable("id") String id) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(id);
		actionsService.endTurn(table);
		tableService.storeTable(id, table);
	}
}
