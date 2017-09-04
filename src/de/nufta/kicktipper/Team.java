package de.nufta.kicktipper;

import org.goochjs.glicko2.Rating;

public class Team {

    private final String name;
    private Rating rating;

    public Team(String name, Rating rating) {
        this.name = name;
        this.rating = rating;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the elo
     */
    public Rating getRating() {
        return rating;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Team other = (Team) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }         
    
}
