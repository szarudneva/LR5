package game.core;

import game.model.*;
import game.timer.*;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private GameField gameField;
    private Saboteur saboteur;
    private TimerFactory timerFactory;
    private MillisecondTimer millisecondTimer;
    private TickTimer tickTimer;
    private boolean running = false;
    private boolean paused = false;        // коммит1
    private Thread gameThread;  //коммит2
    private boolean testMode = false; //коммит3

    private long startTime;
    private long pausedTime;               //коммит4
    private int moveCount;
    private int elapsedSeconds;

    public Game() {
        this(false);
    }

    public Game(boolean testMode) {
        this.testMode = testMode;
        this.moveCount = 0;
        this.elapsedSeconds = 0;
        this.paused = false;
        this.timerFactory = new TimerFactory();
    }

    public void start() {
        timerFactory.create();
        millisecondTimer = timerFactory.getMillisecondTimer();
        tickTimer = timerFactory.getTickTimer();

        gameField = new GameField(4, 4);
        saboteur = new SimpleSaboteur(gameField);
        saboteur.start();

        startTime = System.currentTimeMillis();
        running = true;
        paused = false;

        if (!testMode) {
            gameThread = new Thread(this::runGameLoop);
            gameThread.start();

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(() -> {
                        placeFirstMine();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // НОВЫЙ МЕТОД - поставить на паузу
    public void pauseGame() {
        if (running && !paused) {
            paused = true;
            pausedTime = System.currentTimeMillis();
            timerFactory.pauseAllTimers();
            System.out.println("Игра на паузе");
        }
    }

    // НОВЫЙ МЕТОД - продолжить игру
    public void resumeGame() {
        if (running && paused) {
            paused = false;
            long pauseDuration = System.currentTimeMillis() - pausedTime;
            startTime += pauseDuration;

            timerFactory.resumeAllTimers();
            System.out.println("Игра продолжена");
        }
    }

    // НОВЫЙ МЕТОД - проверить, на паузе ли игра
    public boolean isPaused() {
        return paused;
    }

    private void placeFirstMine() {
        if (paused) return;

        System.out.println("Пытаемся установить ПЕРВУЮ мину в клетку с цифрой...");

        List<Cell> cellsWithTiles = new ArrayList<>();
        for (int y = 0; y < gameField.getHeight(); y++) {
            for (int x = 0; x < gameField.getWidth(); x++) {
                Cell cell = gameField.getCell(x, y);
                if (cell.getTile() != null) {
                    boolean hasMine = false;
                    for (Unit unit : cell.getUnits()) {
                        if (unit instanceof FreezeMine) {
                            hasMine = true;
                            break;
                        }
                    }
                    if (!hasMine) {
                        cellsWithTiles.add(cell);
                    }
                }
            }
        }

        if (!cellsWithTiles.isEmpty()) {
            Random random = new Random();
            Cell selectedCell = cellsWithTiles.get(random.nextInt(cellsWithTiles.size()));

            FreezeMine mine = new FreezeMine(5000, gameField, timerFactory);
            selectedCell.putUnit(mine);
            mine.setOwner(selectedCell);
            mine.activate();
            System.out.println("ПЕРВАЯ мина установлена в клетке с цифрой " + selectedCell.getTile().getNumber());
        }
    }

    private void runGameLoop() {
        while (running && !isOver()) {
            try {
                Thread.sleep(1000);
                if (!paused) {
                    updateElapsedTime();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateElapsedTime() {
        elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    public void stop() {
        running = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
        updateElapsedTime();
    }

    public boolean isOver() {
        return saboteur != null && saboteur.checkTilesInFinishConfiguration();
    }

    public void incrementMoves() {
        if (!paused) {
            moveCount++;
        }
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int getElapsedSeconds() {
        if (running && !paused) {
            updateElapsedTime();
        }
        return elapsedSeconds;
    }

    public GameField getGameField() { return gameField; }
    public MillisecondTimer getMillisecondTimer() { return millisecondTimer; }
    public TickTimer getTickTimer() { return tickTimer; }
    public TimerFactory getTimerFactory() { return timerFactory; }
}