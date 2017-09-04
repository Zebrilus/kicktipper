package de.nufta.kicktipper;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;

public class TeamRepository {

    Hashtable<String, Team> teams = new Hashtable<String, Team>();
    private static TeamRepository instance = new TeamRepository();
    
    private TeamRepository() {       
    }
    
    public static TeamRepository getInstance() {
        return instance;
    }
    
    /**
     * Gets (and creates if necessary) a Team
     * @param name
     */
    public Team getTeam(String name, Season previousSeason, RatingCalculator calc) {
        Team team = teams.get(name);
        if (team == null) {
            if (previousSeason == null) {
                team = new Team(name, new Rating(name,calc));
                teams.put(name, team);
            } else {
                team = new Team(name, new Rating(name, calc, previousSeason.getRelegationRating(), calc.getDefaultRatingDeviation(), calc.getDefaultVolatility()));
                teams.put(name, team);
            }
        }
        return team;
    }
    
    public List<Team> getAllTeams() {
        return new ArrayList<Team>(teams.values());
    }

}
