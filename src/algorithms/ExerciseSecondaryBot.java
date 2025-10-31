// Fichier : ExerciseSecondaryBot.java
// VERSION "THE BLITZ" (Assaut Agressif 600-800)
package algorithms;

import java.util.ArrayList;
import java.util.Random;
import robotsimulator.Brain; // <-- CET IMPORT DOIT MARCHER
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class ExerciseSecondaryBot extends Brain { // <-- CECI DOIT MARCHER

    // --- ÉTATS ---
    private static final int STATE_SEARCH = 0;
    private static final int STATE_ATTACK = 1;
    private static final int STATE_AVOIDING = 2; // État de déblocage

    // --- Constantes ---
    private static final int FIRE_COOLDOWN_TIME = 21;
    // --- DISTANCES AGRESSIVES ("THE BLITZ") ---
    private static final double MAX_ATTACK_DIST = 800.0; // PUSH
    private static final double MIN_ATTACK_DIST = 600.0; // PUSH
    private static final double AIM_PRECISION = 0.02;
    private static final int AVOIDANCE_STEPS = 25; // Tourne pendant 25 steps

    // --- Variables ---
    private int state;
    private int fireCooldown;
    private int stepCounter;
    private Random randomGenerator;
    private int avoidanceCounter; // Compteur pour le déblocage

    public ExerciseSecondaryBot() {
        super();
        randomGenerator = new Random();
    }

    // @Override
    public void activate() {
        state = STATE_SEARCH;
        fireCooldown = 0;
        stepCounter = 0;
        avoidanceCounter = 0;
        move();
    }

    // @Override
    public void step() {
        stepCounter++;
        if (fireCooldown > 0)
            fireCooldown--;
        if (getHealth() <= 0)
            return;

        // --- GESTION DES ÉTATS PRIORITAIRES ---
        if (state == STATE_AVOIDING) {
            avoidanceCounter--;
            if (avoidanceCounter <= 0) {
                state = STATE_SEARCH;
                move();
                return;
            } else {
                stepTurn(Parameters.Direction.LEFT);
                return;
            }
        }

        IFrontSensorResult front = detectFront();
        boolean isObstacle = (front.getObjectType() != IFrontSensorResult.Types.NOTHING);
        boolean isEnemy = (front.getObjectType() == IFrontSensorResult.Types.OpponentMainBot ||
                front.getObjectType() == IFrontSensorResult.Types.OpponentSecondaryBot);

        if (isObstacle && !isEnemy) {
            state = STATE_AVOIDING;
            avoidanceCounter = AVOIDANCE_STEPS;
            stepTurn(Parameters.Direction.LEFT);
            return;
        }

        // --- Logique normale ---
        IRadarResult closestEnemy = findClosestEnemy(detectRadar());

        if (closestEnemy == null) {
            state = STATE_SEARCH;
        } else {
            state = STATE_ATTACK;
        }

        if (state == STATE_ATTACK) {
            performAttack(closestEnemy);
        } else {
            performSearch();
        }
    }

    private void performSearch() {
        if (randomGenerator.nextDouble() < 0.01) {
            boolean turnRight = randomGenerator.nextBoolean();
            stepTurn(turnRight ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT);
        } else {
            move();
        }
    }

    private void performAttack(IRadarResult enemy) {
        double enemyDir = enemy.getObjectDirection();
        double enemyDist = enemy.getObjectDistance();

        if (isAimed(enemyDir) && fireCooldown <= 0) {
            fire(enemyDir);
            fireCooldown = FIRE_COOLDOWN_TIME;
        } else if (!isAimed(enemyDir)) {
            turnTo(enemyDir);
        } else if (enemyDist > MAX_ATTACK_DIST) { // PUSH
            move();
        } else if (enemyDist < MIN_ATTACK_DIST) { // PUSH
            moveBack();
        } else {
            if (stepCounter % 40 < 20) {
                stepTurn(Parameters.Direction.RIGHT);
            } else {
                stepTurn(Parameters.Direction.LEFT);
            }
        }
    }

    // --- Fonctions utilitaires ---
    private boolean isAimed(double direction) {
        return Math.abs(Math.sin(getHeading() - direction)) < AIM_PRECISION;
    }

    private IRadarResult findClosestEnemy(ArrayList<IRadarResult> radarResults) {
        IRadarResult closest = null;
        double minDistance = Double.MAX_VALUE;
        if (radarResults == null)
            return null;

        for (IRadarResult r : radarResults) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                    r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                double dist = r.getObjectDistance();
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = r;
                }
            }
        }
        return closest;
    }

    private void turnTo(double direction) {
        double ref = direction - getHeading();
        while (ref > Math.PI)
            ref -= 2 * Math.PI;
        while (ref <= -Math.PI)
            ref += 2 * Math.PI;

        if (ref > 0)
            stepTurn(Parameters.Direction.RIGHT);
        else
            stepTurn(Parameters.Direction.LEFT);
    }
}