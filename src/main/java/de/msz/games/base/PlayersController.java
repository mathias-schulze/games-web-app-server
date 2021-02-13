package de.msz.games.base;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.base.PlayerService.Player;

@RestController
@RequestMapping("/players")
public class PlayersController {
	
	@Autowired
	private PlayerService playerService;
	
	@GetMapping
	public List<Player> getPlayer(@RequestParam List<String> ids) throws InterruptedException, ExecutionException {
		return playerService.getPlayers(ids);
	}
}
