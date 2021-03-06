
package de.nufta.kicktipper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.goochjs.glicko2.RatingCalculator;

/**
 * Parse a season's result data file (or a prediction file with the same format)
 * 
 * @author ulrich.luebke
 */
public class SeasonParser {

    public static final boolean PREDICT_MODE = true;
    public static final boolean SEASON_MODE = false;

    private enum Stage {
        INIT, GAME, HOME, AWAY, SCORE
    };

    private final RatingCalculator calc;
    private final Season previousSeason;
    private Stage stage = Stage.INIT;
    private final int year;
    private final String fileName;
    private final Season season;
    private int day = 0;
    private boolean predictMode;
    private Team homeTeam, awayTeam;
    private int homeScore, awayScore;
    private String date;
    private int lineNumber = 0;

    private String tournamentPhaseName;
    private int tournamentPhaseNumber = 10101;

    private static final Pattern FINALS_PATTERN = Pattern.compile("^Finale.*");
    private static final Pattern FINALS_THIRD_PLACE_PATTERN = Pattern.compile("^3\\. Platz.*");
    private static final Pattern SEMI_FINALS_PATTERN = Pattern.compile("^Halbfinale");
    private static final Pattern QUARTER_FINALS_PATTERN = Pattern.compile("^Viertelfinale");
    private static final Pattern ROUND_OF_SIXTEEN_PATTERN = Pattern.compile("^Achtelfinale");

    private static final Pattern[] FINALS_PATTERNS = { ROUND_OF_SIXTEEN_PATTERN, QUARTER_FINALS_PATTERN,
            SEMI_FINALS_PATTERN, FINALS_THIRD_PLACE_PATTERN, FINALS_PATTERN };

    static final int FINALS_OFFSET = 1000;
    static final String[] FINALS_NAMES = { "Achtelfinale", "Viertelfinale", "Halbfinale", "3. Platz", "Finale" };

    private static final Pattern TOURNAMENT_PART_PATTERN = Pattern.compile("^(Turnierbaum|Tabelle).*");

    private static final Pattern SPIELTAG_PATTERN = Pattern.compile(".*Spieltag.*");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}+\\.\\d{1,2}+\\..*\\d{1,2}+:\\d{1,2}+.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_PATTERN = Pattern.compile("\\d{1,2}+\\s*:\\s*\\d{1,2}+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_AFTER_90_MIN_PATTERN = Pattern.compile("\\(.*\\)");
    private static final Pattern NO_RESULT_PATTERN = Pattern.compile("^-.*");

    /**
     * Create a new Instance
     */
    SeasonParser(int year, String fileName, boolean predictMode, RatingCalculator calc,
            Season previousSeason) {
        this.year = year;
        this.fileName = fileName;
        this.predictMode = predictMode;
        this.calc = calc;
        this.previousSeason = previousSeason;
        season = new Season(year, calc);
        init();
    }

    private void init() {
        homeTeam = awayTeam = null;
        homeScore = awayScore = -1;
        date = null;
    }

    public Season parse() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream("data/" + fileName)))) {
            String line;
            while ((line = br.readLine()) != null) {
                ++lineNumber;
                processLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error while parsing File \"" + fileName + "\" line " + lineNumber);
            e.printStackTrace();
            System.exit(66);
        } catch (Throwable e) {
            System.err.println("Error while parsing File \"" + fileName + "\" line " + lineNumber);
            e.printStackTrace();
            System.exit(65);
        }
        if (stage == Stage.SCORE && predictMode) {
            processLine("");
        }
        return season;
    }

    private void processLine(String line) {
        switch (KickTipper.getMode()) {
            case WORLD_CUP:
                processLineWorldCup(line);
                break;
            case LEAGUE:
                processLineLeague(line);
                break;
            default:
                System.err.println("Unknown Mode");
                new IllegalStateException().printStackTrace();
                System.exit(99);
        }
    }

    private void processLineWorldCup(final String line) {
        if (TOURNAMENT_PART_PATTERN.matcher(line).matches()) {
            newTournamentPart(line);
            tournamentPhaseNumber--;
            return;
        }
        int pos = 0;
        for (Pattern pattern : FINALS_PATTERNS) {
            if (pattern.matcher(line).matches()) {
                newDay(line, FINALS_OFFSET + pos);
                return;
            }
            pos++;
        }
        // ignore for now - we're after 120 minutes results
        if (RESULT_AFTER_90_MIN_PATTERN.matcher(line).matches()) {
            return;
        }
        processLineLeague(line);
    }

    private void processLineLeague(String line) {
        if (SPIELTAG_PATTERN.matcher(line).matches()) {
            newDay(line);
        } else if (DATE_PATTERN.matcher(line).matches()) {
            newGame(line);
        } else if (RESULT_PATTERN.matcher(line).matches()) {
            result(line);
        } else if (NO_RESULT_PATTERN.matcher(line).matches()) {
            noResultRegistered(line);
        } else if (line.isEmpty()) {
            noResult(line);
        } else {
            teamName(line);
        }
    }

    private static final String[] tpStarts = { "Tabelle", "Turnierbaum" };

    private void newTournamentPart(String line) {
        assert (stage == Stage.INIT || stage == Stage.GAME);
        boolean found = false;
        for (String start : tpStarts) {
            if (line.startsWith(start)) {
                line = line.substring(start.length());
                found = true;
                break;
            }
        }
        assert found;
        this.tournamentPhaseName = line;
        this.tournamentPhaseNumber--;

    }

    private void result(String line) {
        assert (stage == Stage.SCORE);
        assert (predictMode == false);
        String[] split = line.split(":");
        homeScore = Integer.parseInt(split[0].trim());
        awayScore = Integer.parseInt(split[1].trim());
        Game game;
        switch (KickTipper.getMode()) {
            case LEAGUE:
                game = new Game(date, year, day, homeTeam, awayTeam, homeScore, awayScore);
                break;
            case WORLD_CUP:
                game = new Game(tournamentPhaseName, tournamentPhaseNumber, date, year, day, homeTeam, awayTeam,
                        homeScore, awayScore);
                WorldRepository.getInstance().register(tournamentPhaseName, homeTeam.getName(), awayTeam.getName());
                break;
            default:
                assert (false);
                System.exit(66);
                game = null;
        }
        season.addGame(game);
        init();
        stage = Stage.GAME;
    }

    private void noResultRegistered(String line) {
        assert (stage == Stage.SCORE);
        assert (predictMode == false);
        init();
        stage = Stage.GAME;
    }

    private void noResult(String line) {
        assert (stage == Stage.SCORE);
        assert (predictMode == true);
        Game game;
        switch (KickTipper.getMode()) {
            case LEAGUE:
                game = new Game(date, year, day, homeTeam, awayTeam, -1, -1);
                break;
            case WORLD_CUP:
                game = new Game(tournamentPhaseName, tournamentPhaseNumber, date, year, day, homeTeam, awayTeam, -1,
                        -1);
                break;
            default:
                assert (false);
                System.exit(66);
                game = null;
        }

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
        this.date = line;
        stage = Stage.HOME;
    }

    private void newDay(String line, int dayNum) {
        assert (stage == Stage.INIT || stage == Stage.GAME);
        day = dayNum;
        stage = Stage.GAME;
    }

    private void newDay(String line) {
        assert (stage == Stage.INIT || stage == Stage.GAME);
        String[] split = line.split("\\.");
        int newDay = Integer.parseInt(split[0]);
        day = newDay;
        stage = Stage.GAME;
    }

    public static void main(String[] args) {
        System.out.println(SPIELTAG_PATTERN.matcher("2342. Spieltag      nnn").matches());
        System.out.println(DATE_PATTERN.matcher("07.05. 11:00").matches());
        System.out.println(DATE_PATTERN.matcher("7.05. 1:00  mm").matches());
        System.out.println(RESULT_PATTERN.matcher("1 : 3").matches());
        System.out.println(RESULT_PATTERN.matcher("1: 3").matches());
        System.out.println(RESULT_PATTERN.matcher("1: 3").matches());
        System.out.println(RESULT_PATTERN.matcher("1:   30").matches());
    }

}
