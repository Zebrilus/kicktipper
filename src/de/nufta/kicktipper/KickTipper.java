
package de.nufta.kicktipper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.goochjs.glicko2.RatingCalculator;

import de.nufta.kicktipper.Game.Result;

/**
 * @author ulrich.luebke
 *
 */
public class KickTipper {

    enum Mode{LEAGUE, WORLD_CUP};
    
//    private static final String[] SEASON_NAMES = { "blfr_2013_res.txt", "blfr_2014_res.txt", "blfr_2015_res.txt",
//            "blfr_2016_res.txt", "blfr_2017_res.txt" };
//    private static final int FIRST_YEAR = 2013;
//    private static final int PREDICT_YEAR = 2017;

//    private static final String PRED_GAMES = "blfr_pred.txt";
    
    private final Mode mode;
    private final String tournamentName ;
    private final String tournamentShortName ;
    private final int startYear ;
    private final int endYear ;
    private final int predictionYear;
    
    /**
     * Show debug printouts on console
     */
    private boolean debug = false;
    

    List<Season> seasons = new ArrayList<Season>();
    RatingCalculator calc = new RatingCalculator();
    Season predictions;   

    /**
     * Creates a new Instance 
     * 
     * @param mode Tournament Mode (Necessary for fileformat parsing) // Data copy-pasted from flashscore.de
     * @param tournamentName name of the tournament
     * @param tournamentShortName short name for identifying files ("blfr" -> Bundesliga Frauen "wm" -> Fußballweltmeisterschaft)
     * @param fromYear use historical data beginning with this year
     * @param toYear use historical data up to and including this year
     * @param predictionYear year of the tournament to predict
     */
    KickTipper(final Mode mode, 
                    final String tournamentName, 
                    final String tournamentShortName, 
                    final int startYear, 
                    final int endYear, 
                    final int predictionYear) {
        
        
        this.mode = mode;
        this.tournamentName = tournamentName;
        this.tournamentShortName = tournamentShortName;
        this.startYear = startYear;
        this.endYear = endYear;
        this.predictionYear = predictionYear;        
    }
    
    private void run() {
        loadSeasons();
        loadPredictions();
    }
    
    /**
     * Enable / disable debug output
     */
    void setDebug (boolean b) {
        this.debug = b;
    }
    
    boolean isDebug() {
        return debug;
    }

    private void loadSeasons() {
        int year = startYear;
        Season previousSeason = null;
        final String resultFilePrefix = tournamentShortName + "_";
        final String resultFilePostfix = "_res.txt";        
        while (year <= endYear) {
            final String seasonFileName = resultFilePrefix + year + resultFilePostfix; 
            SeasonParser parser = new SeasonParser(this, year, seasonFileName, SeasonParser.SEASON_MODE, calc,
                    previousSeason);
            switch(mode) {
                case LEAGUE: {
                    year++; 
                    break;
                        }
                case WORLD_CUP: {
                    year += 4;
                    break;
                }
            }
            Season s = parser.parse();
            s.rate();
            System.out.println("SeasonParser: " + s.toString() );
            seasons.add(s);
            previousSeason = s;
        }
    }

    private void loadPredictions() {
        String predictionFileName = tournamentShortName + "_pred.txt";
        SeasonParser parser = new SeasonParser(
                    this,
                    predictionYear,
                    predictionFileName, 
                    SeasonParser.PREDICT_MODE, 
                    calc, 
                    seasons.get(seasons.size()-1));
        predictions = parser.parse();
        ArrayList<Game>allGames = new ArrayList<Game>();
        for (Season season : seasons) {
            // In the first season the glicko rating is not yet accurate, so leave that out.
            if (season.year > this.startYear) {
                allGames.addAll(season.getGames());
            }
        }
        List<Game> predGames = predictions.getGames();
        for (Game pGame : predGames) {
            double rating1 = pGame.getTeam1().getRating().getRating();
            pGame.setRatingBefore1(rating1);
            double rating2 = pGame.getTeam2().getRating().getRating();
            pGame.setRatingBefore2(rating2);
            final double diff = pGame.getRatingDifference();
            //System.out.println(diff);
            allGames.sort((g1,  g2) -> {
                    double diff1 = g1.getRatingDifference();
                    double diff2 = g2.getRatingDifference();
                    double d1 = Math.abs(diff-diff1);
                    double d2 = Math.abs(diff-diff2);                   
                    int compareTo = new Double(d1).compareTo(new Double(d2));
                    //System.out.println(g1 + " " + g2+ " " + diff1 + " " + diff2 + " " +compareTo);
                    return compareTo;
                });
            int target = 6;
            int cnt = 0;
            final int maxQualityDifference = 20;
                        
            List<Game> selected = new ArrayList<>();
            
            int[] results = new int[Game.Result.values().length];  //3!
            for (Game mGame : allGames) {
                double tDiff = mGame.getRatingDifference();
                double quality = Math.abs(tDiff-diff);
                if (cnt++ >=target && quality > maxQualityDifference) {
                    break;
                } 
                Result result = mGame.getResult();
                results[result.ordinal()]++;
                selected.add(mGame);                
            }
            int leeway = selected.size() / 5;
            int max = Math.max(results[0],Math.max(results[1],results[2]));
//            System.out.println("Leeway: " + leeway);
            for (Iterator<Game> i = selected.iterator();i.hasNext();) {
                Game game = i.next();
                if (results[game.getResult().ordinal()] < max-leeway) {
                    i.remove();
                }
            }
                        
            double home = 0, away = 0;
            final double gameBaseFactor = 1.0;
            final double qualityBaseFactor = 0.5;
            double divider = 0;
            for (Game mGame : selected) {                
//                double tRating1 = mGame.getTeam1().getRating().getRating();
//                double tRating2 = mGame.getTeam2().getRating().getRating();
//                final double tDiff = tRating1 - tRating2;
                double tDiff = mGame.getRatingDifference();
                double quality = Math.abs(tDiff-diff);
                if (cnt++ >=target && quality > maxQualityDifference) {
                    break;
                }
                double qualityfactor = (maxQualityDifference-quality) * (qualityBaseFactor/maxQualityDifference);
                System.out.print("Quality: " + (Math.round(quality))+ ", Factor " + new DecimalFormat("0.00").format(gameBaseFactor+qualityfactor) +" ->");
                System.out.println(mGame);
                home += (mGame.getScore1() * (gameBaseFactor+qualityfactor));
                away += (mGame.getScore2() * (gameBaseFactor+qualityfactor)) ;
                divider += (gameBaseFactor +qualityfactor);
            }
            home /= divider;
            away /= divider;
            double goalDiff = away -home;
            pGame.setScore1((int)Math.round(home));
            pGame.setScore2((int)Math.round(pGame.getScore1()+goalDiff));
            System.out.println("----> Predicted Result: " + pGame + "  GoalDiff:" +new DecimalFormat("0.00").format(goalDiff));
            System.out.println("\n\n");
        }
        System.out.println("Predictions: " + predictions.toString());
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        KickTipper tipper = new KickTipper(Mode.LEAGUE, "Bundesliga Frauen", "blfr", 2013, 2017, 2017);
        tipper.run();
    }


}
