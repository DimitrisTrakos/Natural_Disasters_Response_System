package mapGrid;

public class GridCell {
    public boolean isObstacle;
    public boolean hasVictim;
    public boolean hasAgent;
    public String agentType; // ex. "drone", "firefighter"
    public int riskLevel;

    public boolean isForest;
    public boolean isOnFire;

    public GridCell() {
        this.isObstacle = false;
        this.hasVictim = false;
        this.hasAgent = false;
        this.agentType = "";
        this.riskLevel = 0;
        this.isForest = false;
        this.isOnFire = false;
    }
}
