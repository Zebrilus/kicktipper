
package de.nufta.kicktipper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.goochjs.glicko2.RatingCalculator;

/**
 * @author ulrich.luebke
 *
 */
public class SeasonParser {
    
    public static final boolean PREDICT_MODE = true;
    public static final boolean SEASON_MODE = false;
    
    private enum Stage {INIT, GAME, HOME, AWAY, SCORE};
        
    private final RatingCalculator calc;
    private Season previousSeason;
    private Stage stage = Stage.INIT;
    private int year;
    private String fileName;
    private Season season;
    private int day = 0;
    private boolean predictMode;
    private Team homeTeam, awayTeam;
    private int homeScore, awayScore;
    
    private static Pattern spielTagPattern = Pattern.compile(".*SpielTag.*", Pattern.CASE_INSENSITIVE);
    private static Pattern datePattern = Pattern.compile("\\d{1,2}+\\.\\d{1,2}+\\..*\\d{1,2}+:\\d{1,2}+.*", Pattern.CASE_INSENSITIVE);
    private static Pattern resultPattern = Pattern.compile("\\d{1,2}+\\s*:\\s*\\d{1,2}+", Pattern.CASE_INSENSITIVE);
    
    /**
     * 
     */
    public SeasonParser(int year, String fileName, boolean predictMode, RatingCalculator calc, Season previousSeason) {
        this.year = year;
        this.fileName = fileName;
        this.predictMode = predictMode;
        this.calc = calc;
        this.previousSeason = previousSeason;
        init();
    }
    
    private void init() {
        homeTeam = awayTeam = null;
        homeScore = awayScore = -1;
    }
    
    public Season parse() {
        season = new Season(year, calc);
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("data/"+fileName)))) {
            String line;
            while ((line = br.readLine()) != null) {
               processLine(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } 
        if (stage == Stage.SCORE && predictMode) {
            processLine("");
        }
        return season;
    }

    private void processLine(String line) {
        if (spielTagPattern.matcher(line).matches()) {
            newDay(line);
        } else if (datePattern.matcher(line).matches()) {
            newGame(line);
        } else if (resultPattern.matcher(line).matches()) {
            result(line);
        } else if (line.isEmpty()) {
            noResult(line);
        } else {
            teamName(line);
        }
    }
    
    private void result(String line) {
        assert (stage == Stage.SCORE);
        assert (predictMode == false);
        String[] split = line.split(":");
        homeScore = Integer.parseInt(split[0].trim());
        awayScore =Integer.parseInt(split[1].trim());
        Game game = new Game(year, day, homeTeam, awayTeam, homeScore, awayScore);
        season.addGame(game);
        init();
        stage = Stage.GAME;
    }
    
    private void noResult(String line) {
        assert (stage == Stage.SCORE);
        assert (predictMode == true);
        Game game = new Game (year, day, homeTeam, awayTeam, -1, -1);
        season.addGame(game);
        init();
        stage = Stage.GAME;
    }

    private void teamName(String line) {
        assert (stage == Stage.HOME || stage == Stage.AWAY);
        if (stage == Stage.HOME) {
            homeTeam = TeamRepository.getInstance().getTeam(line, previousSeason, calc);
            stage = Stage.AWAY;
        } else if (stage == Stage.AWAY) {
            awayTeam = TeamRepository.getInstance().getTeam(line, previousSeason, calc);
            stage = Stage.SCORE;
        }
    }

    private void newGame(String line) {
        assert (stage == Stage.GAME);
        stage = Stage.HOME;
    }

    private void newDay(String line) {
        assert (stage == Stage.INIT || stage == Stage.GAME);
        String[] split = line.split("\\.");
        int newDay = Integer.parseInt(split[0]);
        day = newDay;
        stage = Stage.GAME;
    }
  
    public static void main(String[] args) {
        System.out.println(spielTagPattern.matcher("2342. Spieltag      nnn").matches());
        System.out.println(datePattern.matcher("07.05. 11:00").matches());
        System.out.println(datePattern.matcher("7.05. 1:00  mm").matches());
        System.out.println(resultPattern.matcher("1 : 3").matches());
        System.out.println(resultPattern.matcher("1: 3").matches());
        System.out.println(resultPattern.matcher("1: 3").matches());
        System.out.println(resultPattern.matcher("1:   30").matches());
    }
    
}
