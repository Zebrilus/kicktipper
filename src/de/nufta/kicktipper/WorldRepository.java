package de.nufta.kicktipper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

/**
 * An attempt to create correctional values for FIFA regions
 * 
 * @author ulrich.luebke
 */
public class WorldRepository {

    static final String AFRICA = "AFRIKA";
    static final String NORTH_AMERICA = "NORD- & MITTELAMERIKA";
    static final String SOUTH_AMERICA = "SÜDAMERIKA";
    static final String AUSTRALIA = "AUSTRALIEN & OZEANIEN";
    static final String ASIA = "ASIEN";
    static final String EUROPE = "EUROPA";

    static final String[] regions = { AFRICA, NORTH_AMERICA, SOUTH_AMERICA, AUSTRALIA, ASIA, EUROPE };

    private final HashMap<String, Team> regionTeamMap = new HashMap<>();
    /** Maps Country (team name) to region */
    private final HashMap<String, String> worldMap = new HashMap<>();

    RatingCalculator worldCalc = new RatingCalculator();

    private static final WorldRepository instance = new WorldRepository();

    private WorldRepository() {}

    static WorldRepository getInstance() {
        return instance;
    }

    void register(final String phaseName, final String teamName1, final String teamName2) {
        for (String region : regions) {
            if (phaseName.contains(region)) {
                if (region.equals(AUSTRALIA)) {
                    region = ASIA;
                }
                worldMap.put(teamName1, region);
                worldMap.put(teamName2, region);
                break;
            }
        }
    }

    void rate(final String teamName1, final String teamName2, final int score1, final int score2) {
        String region1 = worldMap.get(teamName1);
        String region2 = worldMap.get(teamName2);
        if (region1 != null && region2 != null && (!region1.equals(region2))) {
            Team team1 = getTeam(region1);
            Team team2 = getTeam(region2);
            Rating rating1 = team1.getRating();
            Rating rating2 = team2.getRating();
            RatingPeriodResults rpr = new RatingPeriodResults();
            int result = score1 - score2;
            if (result == 0) {
                rpr.addDraw(rating1, rating2);
            } else if (result > 0) {
                rpr.addResult(rating1, rating2);
            } else { // (result < 0)
                rpr.addResult(rating2, rating1);
            }
            worldCalc.updateRatings(rpr);
        }
    }
    
    double getCorrection (String team1, String team2) {
        if (worldMap.containsKey(team1) && worldMap.containsKey(team2)) {
            double rating1 = regionTeamMap.get(worldMap.get(team1)).getRating().getRating();
            double rating2 = regionTeamMap.get(worldMap.get(team2)).getRating().getRating();
            return (rating1-rating2);
        }
        return 0;
    }

    private Team getTeam(String region) {
        Team team = regionTeamMap.get(region);
        if (team == null) {
            team = new Team(region, new Rating(region, worldCalc));
            regionTeamMap.put(region, team);
        }
        return team;
    }
    
    String getRegions() {
        StringBuilder sb = new StringBuilder("Regions: \n");
        Set<String> countries  = worldMap.keySet();
        for (String country: countries) {
            sb.append(country).append(" -> ").append(worldMap.get(country)).append("\n");
        }
        return sb.toString();
    }
    
    String getRanking() {
        StringBuilder sb = new StringBuilder("Ranking Regions: \n");
        List<Team> allTeams = new ArrayList<Team>(this.regionTeamMap.values());
        allTeams.sort((t1, t2) -> {
            return new Double(t2.getRating().getRating()).compareTo(new Double(t1.getRating().getRating()));
        });;
        for (Team team : allTeams) {
                sb.append(team).append("\n");
        }
        return sb.toString();
    }

}
