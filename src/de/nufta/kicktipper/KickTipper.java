
package de.nufta.kicktipper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.goochjs.glicko2.RatingCalculator;

import de.nufta.kicktipper.Game.Result;

/**
 * @author ulrich.luebke
 *
 */
public class KickTipper {

    enum Mode {
        LEAGUE, WORLD_CUP
    };

    // private static final String[] SEASON_NAMES = { "blfr_2013_res.txt", "blfr_2014_res.txt", "blfr_2015_res.txt",
    // "blfr_2016_res.txt", "blfr_2017_res.txt" };
    // private static final int FIRST_YEAR = 2013;
    // private static final int PREDICT_YEAR = 2017;

    // private static final String PRED_GAMES = "blfr_pred.txt";

    private static Mode mode;
    private final String tournamentName;
    private final String tournamentShortName;
    private final int startYear;
    private final int endYear;
    private final int predictionYear;

    /**
     * Show debug printouts on console
     */
    private static boolean debug = false;

    List<Season> seasons = new ArrayList<Season>();
    RatingCalculator calc = new RatingCalculator();
    Season predictions;

    /**
     * Creates a new Instance
     * 
     * @param mode Tournament Mode (Necessary for fileformat parsing) // Data copy-pasted from flashscore.de
     * @param tournamentName name of the tournament
     * @param tournamentShortName short name for identifying files ("blfr" -> Bundesliga Frauen "wm" ->
     *        Fußballweltmeisterschaft)
     * @param fromYear use historical data beginning with this year
     * @param toYear use historical data up to and including this year
     * @param predictionYear year of the tournament to predict
     */
    KickTipper(final String tournamentName, final String tournamentShortName, final int startYear,
            final int endYear, final int predictionYear) {

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
    static void setDebug(boolean b) {
        debug = b;
    }

    static boolean isDebug() {
        return debug;
    }

    private void loadSeasons() {
        int year = startYear;
        Season previousSeason = null;
        final String resultFilePrefix = tournamentShortName + "_";
        final String resultFilePostfix = "_res.txt";
        while (year <= endYear) {
            final String seasonFileName = resultFilePrefix + year + resultFilePostfix;
            SeasonParser parser = new SeasonParser(year, seasonFileName, SeasonParser.SEASON_MODE, calc,
                    previousSeason);
            switch (mode) {
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
            if (mode.equals(Mode.WORLD_CUP)) {
                List<Game> games = s.getGames();
                WorldRepository worldRepository = WorldRepository.getInstance();
                for (Game game : games) {
                    worldRepository.rate(game.getTeam1().getName(), game.getTeam2().getName(), game.getScore1(), game.getScore2());
                }
            }
            if (isDebug()) {
                System.out.println("SeasonParser: " + s.toString());
            }
            seasons.add(s);
            previousSeason = s;
        }
    }

    private void loadPredictions() {
        String predictionFileName = tournamentShortName + "_pred.txt";
        SeasonParser parser = new SeasonParser(predictionYear, predictionFileName, SeasonParser.PREDICT_MODE,
                calc, seasons.get(seasons.size() - 1));
        predictions = parser.parse();
    }

    private void predict() {
        ArrayList<Game> allGames = new ArrayList<Game>();
        for (Season season : seasons) {
            // In the first season the glicko rating is not yet accurate, so leave that out.
            if (season.year > this.startYear) {
                List<Game> games = season.getGames();
                allGames.addAll(games);
                if (Mode.WORLD_CUP.equals(getMode())) {
                    for (Game game : games) {
                        allGames.add(game.createReverseGame());
                    }
                }
            }
        }
        List<Game> predGames = predictions.getGames();
        System.out.println("Predictions for \"" + tournamentName + " " + predictionYear + "\"");
        for (Game pGame : predGames) {
            double rating1 = pGame.getTeam1().getRating().getRating();
            pGame.setRatingBefore1(rating1);
            double rating2 = pGame.getTeam2().getRating().getRating();
            pGame.setRatingBefore2(rating2);
            final double diff =  pGame.getCorrectedRatingDifference();
            allGames.sort((g1, g2) -> {
                double diff1 = g1.getCorrectedRatingDifference();
                double diff2 = g2.getCorrectedRatingDifference();
                double d1 = Math.abs(diff - diff1);
                double d2 = Math.abs(diff - diff2);
                int compareTo = new Double(d1).compareTo(new Double(d2));
                return compareTo;
            });
            int target = 6;
            int cnt = 0;
            final int maxQualityDifference = mode.equals(Mode.LEAGUE) ? 20 : 10;
            final int maxRatingDifference = 700;

            List<Game> selected = new ArrayList<>();
            HashSet<Integer> selectedIDs = new HashSet<>();

            int[] results = new int[Game.Result.values().length];
            for (Game mGame : allGames) {
                double tDiff = mGame.getCorrectedRatingDifference();
                double quality = Math.abs(tDiff - diff);
                double rDiff = Math.abs(mGame.getRatingBefore1()-pGame.getTeam1().getRating().getRating());
                if (cnt++ >= target && (quality > maxQualityDifference || rDiff > maxRatingDifference)) {
                    break;
                }
                Result result = mGame.getResult();
                results[result.ordinal()]++;
                if (!selectedIDs.contains(mGame.getID()) ) {
                    selected.add(mGame);
                    selectedIDs.add(mGame.getID());
                }
            }
            int leeway = selected.size() / 5;
            int max = Math.max(results[0], Math.max(results[1], results[2]));
            for (Iterator<Game> i = selected.iterator(); i.hasNext();) {
                Game game = i.next();
                if (results[game.getResult().ordinal()] < max - leeway) {
                    i.remove();
                }
            }

            double home = 0, away = 0;
            final double gameBaseFactor = 1.0;
            final double qualityBaseFactor = 0.5;
            double divider = 0;
            for (Game mGame : selected) {
                double tDiff = mGame.getCorrectedRatingDifference();
                double quality = Math.abs(tDiff - diff);
                if (cnt++ >= target && quality > maxQualityDifference) {
                    break;
                }
                double qualityfactor = (maxQualityDifference - quality) * (qualityBaseFactor / maxQualityDifference);
                if (debug) {
                    System.out.print("Quality: " + (Math.round(quality)) + ", Factor "
                            + new DecimalFormat("0.00").format(gameBaseFactor + qualityfactor) + " ->");
                    System.out.println(mGame);
                }
                home += (mGame.getScore1() * (gameBaseFactor + qualityfactor));
                away += (mGame.getScore2() * (gameBaseFactor + qualityfactor));
                divider += (gameBaseFactor + qualityfactor);
            }
            home /= divider;
            away /= divider;
            double goalDiff = away - home;
            pGame.setScore1((int) Math.round(home));
            pGame.setScore2((int) Math.round(pGame.getScore1() + goalDiff));
            if (debug) {
                System.out.println("----> Predicted Result: " + pGame + "  GoalDiff:"
                        + new DecimalFormat("0.00").format(goalDiff));
                System.out.println("\n\n");
            }
        }
        System.out.println(predictions.toString());
    }

    public static Mode getMode() {
        return mode;
    }
    
    public void printRanking(String...teamNames) {
        HashSet<String> filterSet = new HashSet<>();
        filterSet.addAll(Arrays.asList(teamNames));
        List<Team> allTeams = TeamRepository.getInstance().getAllTeams();
        allTeams.sort((t1, t2) -> {
            return new Double(t2.getRating().getRating()).compareTo(new Double(t1.getRating().getRating()));
        });;
        for (Team team : allTeams) {
            if (filterSet.isEmpty() || filterSet.contains(team.getName())) {
                System.out.println(team);              
            }
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        boolean debug = true;
        setDebug(debug);
        //setMode(Mode.LEAGUE);
        //KickTipper tipper = new KickTipper( "Bundesliga Frauen", "blfr", 2013, 2017, 2017);
        setMode(Mode.WORLD_CUP);
        KickTipper tipper = new KickTipper("Weltmeisterschaft", "wm", 2002, 2018, 2018);
        tipper.run();
        tipper.predict();
        
        //System.out.println();
        //tipper.printRanking();
        //System.out.println(WorldRepository.getInstance().getRegions());
        //System.out.println(WorldRepository.getInstance().getRanking());
        
//        tipper.printRanking("Russland", "Saudi Arabien", "Uruguay", "Ägypten");
//        System.out.println();
//        tipper.printRanking("Iran","Marokko", "Portugal", "Spanien");
//        System.out.println();
//        tipper.printRanking("Australien", "Dänemark", "Frankreich", "Peru");
//        System.out.println();
//        tipper.printRanking("Argentinien", "Island", "Kroatien", "Dänemark");
//        System.out.println();
//        tipper.printRanking("Brasilien", "Costa Rica", "Schweiz", "Serbien");
//        System.out.println();
//        tipper.printRanking("Deutschland","Mexiko", "Schweden", "Südkorea");
//        System.out.println();
//        tipper.printRanking("Belgien", "England", "Panama", "Tunesien");
//        System.out.println();
//        tipper.printRanking("Japan", "Kolumbien", "Polen", "Senegal");
        
    }

    private static void setMode(final Mode mode) {
        KickTipper.mode = mode;
        
    }
}
