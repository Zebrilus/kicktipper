
package de.nufta.kicktipper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.goochjs.glicko2.RatingCalculator;

/**
 * @author ulrich.luebke
 *
 */
public class KickTipper {

    private static final String[] SEASON_NAMES = { "blfr_2013_res.txt", "blfr_2014_res.txt", "blfr_2015_res.txt",
            "blfr_2016_res.txt", "blfr_2017_res.txt" };
    private static final int FIRST_YEAR = 2013;
    private static final int PREDICT_YEAR = 2017;

    private static final String PRED_GAMES = "blfr_next1.txt";

    List<Season> seasons = new ArrayList<Season>();
    RatingCalculator calc = new RatingCalculator();
    Season predictions;

    KickTipper() {
        loadSeasons();
        loadPredictions();
    }

    void run() {

    }

    private void loadSeasons() {
        int year = FIRST_YEAR;
        Season previousSeason = null;
        for (String season : SEASON_NAMES) {
            SeasonParser parser = new SeasonParser(year++, season, SeasonParser.SEASON_MODE, calc,
                    previousSeason);
            Season s = parser.parse();
            s.rate();
            System.out.println("SeasonParser: " + s.toString() );
            seasons.add(s);
            previousSeason = s;
        }
    }

    private void loadPredictions() {
        SeasonParser parser = new SeasonParser(PREDICT_YEAR,  PRED_GAMES, SeasonParser.PREDICT_MODE, calc, seasons.get(seasons.size()-1));
        predictions = parser.parse();
        ArrayList<Game>allGames = new ArrayList<Game>();
        for (Season season : seasons) {
            allGames.addAll(season.getGames());
        }
        List<Game> predGames = predictions.getGames();
        for (Game pGame : predGames) {
            double rating1 = pGame.getTeam1().getRating().getRating();
            pGame.setRatingBefore1(rating1);
            double rating2 = pGame.getTeam2().getRating().getRating();
            pGame.setRatingBefore2(rating2);
            final double diff = rating1 - rating2;
            //System.out.println(diff);
            allGames.sort(new Comparator<Game>() {
                public int compare(Game g1, Game g2) {
                    double diff1 = (g1.getRatingBefore1() - g1.getRatingBefore2());
                    double diff2 = (g2.getRatingBefore1() - g2.getRatingBefore2());
                    diff1 -= diff;
                    diff2 -= diff;
                    int compareTo = new Double(Math.abs(diff1)).compareTo(new Double(Math.abs(diff2)));
                    //System.out.println(g1 + " " + g2+ " " + diff1 + " " + diff2 + " " +compareTo);
                    return compareTo;
                };
            });
            int cnt = 4;
            int home = 0, away = 0;
            for (int i=0; i <cnt; i++) {
                Game mGame = allGames.get(i);
                home += mGame.getScore1();
                away += mGame.getScore2();
            }
            home /= cnt;
            away /= cnt;
            pGame.setScore1(home);
            pGame.setScore2(away);
        }
        System.out.println("Predictions: " + predictions.toString());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        KickTipper tipper = new KickTipper();
        tipper.run();
    }

}
