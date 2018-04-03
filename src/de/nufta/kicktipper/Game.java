package de.nufta.kicktipper;


public class Game implements Comparable<Game> {

    public enum Result{WON, LOST, DRAW};
    
    static volatile int counter = 0;
    
    final private int season;
    private int day;
    final private Team team1;
    final private Team team2;
    private int score1;
    private int score2;
    private int id = ++counter;

    /**
     * @param score1 the score1 to set
     */
    public void setScore1(int score1) {
        this.score1 = score1;
    }

    /**
     * @param score2 the score2 to set
     */
    public void setScore2(int score2) {
        this.score2 = score2;
    }

    private double ratingBefore1;
    private double ratingBefore2;
    private double ratingAfter1;
    private double ratingAfter2;

    public Game(final int season, final int day, final Team team1, final Team team2, final int score1,
            final int score2) {
        this.day = day;
        this.team1 = team1;
        this.team2 = team2;
        this.score1 = score1;
        this.score2 = score2;
        this.season = season;
    }

    /**
     * @return the season
     */
    public int getSeason() {
        return season;
    }

    public void setDay(int day) {
        this.day = day;
    }

    /**
     * @return the date
     */
    public int getDay() {
        return day;
    }

    /**
     * @return the team1
     */
    public Team getTeam1() {
        return team1;
    }

    /**
     * @return the team2
     */
    public Team getTeam2() {
        return team2;
    }

    /**
     * @return the score1
     */
    public int getScore1() {
        return score1;
    }

    /**
     * @return the score2
     */
    public int getScore2() {
        return score2;
    }

    /**
     * @return the rating1
     */
    public double getRatingBefore1() {
        return ratingBefore1;
    }

    /**
     * @param rating1 the rating1 to set
     */
    public void setRatingBefore1(double rating1) {
        this.ratingBefore1 = rating1;
    }

    /**
     * @return the rating2
     */
    public double getRatingBefore2() {
        return ratingBefore2;
    }

    /**
     * @param rating2 the rating2 to set
     */
    public void setRatingBefore2(double rating2) {
        this.ratingBefore2 = rating2;
    }

    /**
     * @return the ratingAfter1
     */
    public double getRatingAfter1() {
        return ratingAfter1;
    }

    /**
     * @return the ratingAfter2
     */
    public double getRatingAfter2() {
        return ratingAfter2;
    }

    /**
     * @param ratingAfter1 the ratingAfter1 to set
     */
    public void setRatingAfter1(double ratingAfter1) {
        this.ratingAfter1 = ratingAfter1;
    }

    /**
     * @param ratingAfter2 the ratingAfter2 to set
     */
    public void setRatingAfter2(double ratingAfter2) {
        this.ratingAfter2 = ratingAfter2;
    }

    // Standard
    @Override
    public int compareTo(Game o) {
        if (this.day != o.day) return new Integer(this.day).compareTo(new Integer(o.day));
        return new Integer(this.id).compareTo(new Integer(o.id));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + day;
        result = prime * result + id;
        long temp;
        temp = Double.doubleToLongBits(ratingAfter1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ratingAfter2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ratingBefore1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ratingBefore2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + score1;
        result = prime * result + score2;
        result = prime * result + season;
        result = prime * result + ((team1 == null) ? 0 : team1.hashCode());
        result = prime * result + ((team2 == null) ? 0 : team2.hashCode());
        return result;
    }

    /* (non-Javadoc)
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
        Game other = (Game) obj;
        if (day != other.day)
            return false;
        if (id != other.id)
            return false;
        if (Double.doubleToLongBits(ratingAfter1) != Double.doubleToLongBits(other.ratingAfter1))
            return false;
        if (Double.doubleToLongBits(ratingAfter2) != Double.doubleToLongBits(other.ratingAfter2))
            return false;
        if (Double.doubleToLongBits(ratingBefore1) != Double.doubleToLongBits(other.ratingBefore1))
            return false;
        if (Double.doubleToLongBits(ratingBefore2) != Double.doubleToLongBits(other.ratingBefore2))
            return false;
        if (score1 != other.score1)
            return false;
        if (score2 != other.score2)
            return false;
        if (season != other.season)
            return false;
        if (team1 == null) {
            if (other.team1 != null)
                return false;
        } else if (!team1.equals(other.team1))
            return false;
        if (team2 == null) {
            if (other.team2 != null)
                return false;
        } else if (!team2.equals(other.team2))
            return false;
        return true;
    }
    
    public Result getResult() {
        int diff = getScore1() - getScore2();
        if (diff < 0) {
            return Result.LOST;
        } else if (diff > 0) {
            return Result.WON;
        } 
        return Result.DRAW;
    }
    
    public double getRatingDifference() {
        return getRatingBefore1() - getRatingBefore2();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( season + "," +day + ".ST: " +  team1.getName() +"(" +Math.round( ratingBefore1) );
        if (ratingAfter1 > 0) {
            sb.append( "->" +Math.round(ratingAfter1));
        }
        sb.append(")  "); 
        sb.append(score1 + ":" + score2 +"  " + team2.getName() +"(" + Math.round(ratingBefore2));
        if (ratingAfter2 >0) {
            sb.append(  "->" +Math.round(ratingAfter2));
        }
        sb.append( ")  -> diff:" + (Math.round(getRatingDifference()) )) ;
        return sb.toString();
    }

}
