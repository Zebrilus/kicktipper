package de.nufta.kicktipper;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

/**
 * @author ulrich.luebke
 *
 */
public class Season implements Comparable<Season> {

    int year;
    boolean rated = false;
    private RatingCalculator calc;

    SortedSet<Game> games = new TreeSet<Game>();

    public Season(int year, RatingCalculator calc) {
        this.year = year;
        this.calc = calc;
    }

    public void addGame(Game g) {
        games.add(g);
    }

    // Rate this season
    public void rate() {
        if (rated) {
            System.err.println("Season " + year + "already rated");
        } else {
            if (KickTipper.isDebug()) {
                System.out.print("Rating Season" + year +".");
            }
            int day = games.first().getDay();
            int phase = games.first().getTournamentPhaseNumber();
            RatingPeriodResults rpr = new RatingPeriodResults();
            List<Team> allTeams = TeamRepository.getInstance().getAllTeams();
            for (Team team : allTeams) {
                rpr.addParticipants(team.getRating());
            }
            List<Game> dayGames = new ArrayList<Game>();
            for (Game game : games) {
                if (game.getDay() != day || game.getTournamentPhaseNumber() != phase) {
                    day = game.getDay();
                    phase = game.getTournamentPhaseNumber();
                    if (KickTipper.isDebug() ) {
                        System.out.print(".");
                    }
                    calc.updateRatings(rpr);
                    updateAfter(dayGames);
                    dayGames.clear();
                }
                dayGames.add(game);
                Team team1 = game.getTeam1();
                Team team2 = game.getTeam2();
                Rating rating1 = team1.getRating();
                game.setRatingBefore1(rating1.getRating());
                Rating rating2 = team2.getRating();
                game.setRatingBefore2(rating2.getRating());
                int result = game.getScore1() - game.getScore2();
                if (result == 0) {
                    rpr.addDraw(rating1, rating2);
                } else if (result > 0) {
                    rpr.addResult(rating1, rating2);
                } else { // (result < 0)
                    rpr.addResult(rating2, rating1);
                }
            }
            if (KickTipper.isDebug()) {
                System.out.println();
            }
            calc.updateRatings(rpr);
            updateAfter(dayGames);
            dayGames.clear();
        }
        rated = true;
    }

    private void updateAfter(List<Game> games) {
        for (Game game : games) {
            Team team1 = game.getTeam1();
            Team team2 = game.getTeam2();
            game.setRatingAfter1(team1.getRating().getRating());
            game.setRatingAfter2(team2.getRating().getRating());
        }
    }

    /**
     * Gets an average Rating of relegated (worst 4) teams of last season
     * 
     * @return
     */
    public double getRelegationRating() {
        return 1200d;
    }

    @Override
    public int compareTo(Season o) {
        return new Integer(year).compareTo(new Integer(o.year));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + year;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Season other = (Season) obj;
        if (year != other.year)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder( "Season: " + year + ": \n");
        int prevDay = -1;
        int prevPhase = -1;
        for (Game game : games) {
            int day = game.getDay();
            int phase = game.getTournamentPhaseNumber();
            if (day != prevDay || phase != prevPhase) {
                if (phase != 0) {
                    sb.append(game.getTournamentPhaseName()).append(" -- ");
                }
                if (day >= SeasonParser.FINALS_OFFSET) {
                    sb.append(SeasonParser.FINALS_NAMES[day-SeasonParser.FINALS_OFFSET]).append(":\n");
                } else {
                    sb.append(day).append(". Spieltag:\n");
                }
                prevDay = day;
                prevPhase = phase;
            }
            
            sb.append(game.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    List<Game> getGames() {
        ArrayList<Game> all = new ArrayList<Game>();
        all.addAll(games);
        return all;
    }
    
}