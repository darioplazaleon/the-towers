package org.nanii.thetowers.stats;

public enum TopType {
    KILLS("SUM(mp.kills)"),
    POINTS("SUM(mp.points)"),
    WINS("SUM(mp.result = 'WIN')"),
    WINRATE("CAST(SUM(mp.result = 'WIN') AS REAL) / COUNT(*)");

    private final String expression;

    TopType(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }
}
