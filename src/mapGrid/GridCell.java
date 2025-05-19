package mapGrid;

public class GridCell {
    public boolean isObstacle;
    public boolean hasVictim;
    public boolean hasAgent;
    public String agentType; // ex. "drone", "firefighter"
    public int riskLevel;

    public boolean isForest;
    public boolean isOnFire;
    public boolean isHouse;
    public boolean fireFighterExtinguishFire;

    public GridCell() {
        this.isObstacle = false;
        this.hasVictim = false;
        this.hasAgent = false;
        this.agentType = "";
        this.riskLevel = 0;
        this.isForest = false;
        this.isOnFire = false;
        this.fireFighterExtinguishFire = false;
        this.isHouse = false;
    }
}
