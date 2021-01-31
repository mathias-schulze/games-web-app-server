package de.msz.games.base;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.base.Game.GameParameter;

@RestController
@RequestMapping("/games")
public class GamesController {
	
	@GetMapping("/list")
	public List<GameParameter> getGameList() {
		return Arrays.stream(Game.values())
				.map(game -> game.getParameter())
				.collect(Collectors.toList());
	}
}
