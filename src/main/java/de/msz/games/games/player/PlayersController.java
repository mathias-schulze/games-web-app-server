package de.msz.games.games.player;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.games.player.PlayerService.Player;

@RestController
@RequestMapping("/players")
public class PlayersController {
	
	@Autowired
	private PlayerService playerService;
	
	@GetMapping
	public List<Player> getPlayer(@RequestParam List<String> ids) {
		return playerService.getPlayers(ids);
	}
}
