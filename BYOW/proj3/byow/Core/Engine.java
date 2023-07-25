package byow.Core;

import byow.InputDemo.InputSource;
import byow.InputDemo.KeyboardInputSource;
import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.Random;

public class Engine {

    private static class LoadGame implements Serializable {
        /*
        PLAYER ATTRIBUTES
         */
        private int playerX = 0;
        private int playerY = 0;
        private boolean visionHidden = true;
        private int radiation = 0;
        private int health = 100;
        private boolean playerIsAlive = true;
        private TETile underPlayerTile = null;
        private boolean[][] hasVisited;
        private int enemyCount = 0;
        private int pMouseX = 0;
        private int pMouseY = 0;

        private TETile[][] tiles;
        private Random random;

        private boolean isActive = false;

        /** The current working directory. */
        public static final File CWD = new File(System.getProperty("user.dir"));

        // Files for loading a LoadGame
        public static LoadGame loadGame() {
            LoadGame loadgame;
            File inFile = new File("loadgame.txt");
            try {
                ObjectInputStream inp =
                        new ObjectInputStream(new FileInputStream(inFile));
                loadgame = (LoadGame) inp.readObject();
                inp.close();
            } catch (IOException | ClassNotFoundException excp) {
                loadgame = null;
            }
            return loadgame;
        }

        /**
         * Saves a LoadGame to a file for future use.
         */
        public void saveGame() {
            File outFile = new File("loadgame.txt");
            try {
                ObjectOutputStream out =
                        new ObjectOutputStream(new FileOutputStream(outFile));
                out.writeObject(this);
                out.close();
            } catch (IOException excp) {
            }
        }
    }

    private LoadGame load = new LoadGame();
    private TERenderer ter = new TERenderer();

    public static final int WIDTH = 120;
    public static final int HEIGHT = 78;
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] NUMBERS = "1234567890".toCharArray();

    private Font fontBig = new Font("Monaco", Font.BOLD, 50);
    private Font fontSmall = new Font("Monaco", Font.PLAIN, 30);


    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        InputSource inputSource = new KeyboardInputSource();
        ter = new TERenderer();
        Font oldFont = StdDraw.getFont();
        ter.initialize(WIDTH, HEIGHT + 4, 0, 4);
        renderMainMenu();

        while (inputSource.possibleNextInput()) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = inputSource.getNextKey();
                if (c == 'N') {
                    renderAwaitSeed("");
                    String curSeed = "";
                    char c2 = 'a';
                    while (c2 != 'S' || curSeed.equals("")) {
                        // wait for the seed exit
                        c2 = inputSource.getNextKey();

                        // only render number
                        for (char a : NUMBERS) {
                            if (c2 == a) {
                                curSeed += c2;
                                renderAwaitSeed(curSeed);
                            }
                        }
                    }

                    StdDraw.clear(Color.BLACK);

                    // Get the initial tiles
                    load.tiles = interactWithInputString("n" + curSeed + "s");

                    // Set all tiles to !visited
                    load.hasVisited = new boolean[WIDTH][HEIGHT];
                    for (int x = 0; x < WIDTH; x++) {
                        for (int y = 0; y < HEIGHT; y++) {
                            load.hasVisited[x][y] = false;
                        }
                    }
                    load.isActive = true;
                    StdDraw.setPenColor(Color.WHITE);
                    StdDraw.setFont(oldFont);
                    playerInput(inputSource, load.tiles);
                }
                if (c == 'L') {
                    LoadGame l = LoadGame.loadGame();

                    if (l == null || !l.isActive) {
                        // invalid load
                        System.exit(0);
                    } else {
                        load = l;
                        StdDraw.setPenColor(Color.WHITE);
                        StdDraw.setFont(oldFont);
                        playerInput(inputSource, load.tiles);
                    }
                }
                if (c == 'Q') {
                    quitGame();
                    System.exit(0);
                }
            }
        }
    }


    /*
    General routine:
    1. Get the player input
    2. Check if its WASD
    3. Check if the player can move to the new location (!= wall)
    4. Change the tile the player is currently on to underPlayerTile
    5. Set the current under tile to be equal to the tile the player is under
    6. Move the player to the new location
    7. Render
     */

    private TETile[][] playerVision;
    public void playerInput(InputSource inputSource, TETile[][] tiles) {

        boolean skipFirst = true;
        boolean awaitingQuit = false;
        boolean local_alive = load.playerIsAlive;

        while (inputSource.possibleNextInput()) {
            boolean needsRefresh = false;

             playerVision = new TETile[WIDTH][HEIGHT];
             if (local_alive) {
                for (int x = 0; x < WIDTH; x++) {
                    for (int y = 0; y < HEIGHT; y++) {
                        playerVision[x][y] = Tileset.NOTHING;

                        if (load.hasVisited[x][y]) {
                            playerVision[x][y] = tiles[x][y];
                        }
                    }
                }
            }

            if (skipFirst || StdDraw.hasNextKeyTyped()) {
                char c = skipFirst ? 'X' : inputSource.getNextKey();
                skipFirst = false;
                needsRefresh = true;

                // UP
                if (awaitingQuit && c == 'Q') {
                    quitGame();
                    System.exit(0);
                } else if (c == ':') {
                    awaitingQuit = true;
                    continue;
                } else if (c == 'H') {
                    load.visionHidden = !load.visionHidden;
                } else if (!local_alive && c == 'M') {
                    renderMainMenu();
                    return;
                } else if (c == 'R') {
                    // Attack
                    for (int x = load.playerX - 2; x < load.playerX + 2; x++) {
                        for (int y = load.playerY - 2; y < load.playerY + 2; y++) {
                            if (isValid(x, y, 0) && (tiles[x][y].description().equals(Tileset.TREE.description()) || tiles[x][y].description().equals(Tileset.LOCKED_DOOR.description()))) {
                                // Attacking an enemy
                                if (tiles[x][y].description().equals(Tileset.TREE.description())) load.enemyCount--;
                                tiles[x][y] = tiles[x][y].description().equals(Tileset.TREE.description()) ? Tileset.FLOOR : Tileset.UNLOCKED_DOOR;
                            }
                        }
                    }
                }

                if (local_alive && load.enemyCount == 0) {
                    load.playerIsAlive = false;
                    local_alive = false;
                    displayWin();
                    continue;
                }

                awaitingQuit = false;

                if (!local_alive) continue;

                doMovement(c, tiles);

                if (load.health < 10) {
                    load.playerIsAlive = false;
                    local_alive = false;
                    displayDeath();
                    continue;
                }
            }

            if (!local_alive) continue;
            int mouseX = (int) StdDraw.mouseX();
            int mouseY = (int) StdDraw.mouseY() - 4;

            // Refresh if there's been a change
            if (needsRefresh || (isValid(mouseX, mouseY, 0) && ((!load.tiles[load.pMouseX][load.pMouseY].equals(load.tiles[mouseX][mouseY])) && (mouseX != load.pMouseX || mouseY != load.pMouseY)))) {
                ter.renderFrame((load.visionHidden) ? playerVision : tiles);
                renderMouseHUD(mouseX, mouseY, playerVision);
                renderHUD();
            }
        }
    }

    public void doMovement(char c, TETile[][] tiles) {
        int xChange = 0;
        int yChange = 0;

        if (c == 'W') yChange += 1;
        else if (c == 'S') yChange -= 1;
        else if (c == 'A') xChange -= 1;
        else if (c == 'D') xChange += 1;

        // Check for collision
        if ((load.playerY + yChange >= 0 && load.playerY + yChange < HEIGHT) && (load.playerX + xChange >= 0 && load.playerX + xChange < WIDTH) && !load.tiles[load.playerX + xChange][load.playerY + yChange].description().equals(Tileset.WALL.description()) && !load.tiles[load.playerX + xChange][load.playerY + yChange].description().equals(Tileset.LOCKED_DOOR.description())) {
            // Valid move
            tiles[load.playerX][load.playerY] = load.underPlayerTile;
            playerVision[load.playerX][load.playerY] = load.underPlayerTile;
            load.underPlayerTile = tiles[load.playerX + xChange][load.playerY + yChange];
            tiles[load.playerX + xChange][load.playerY + yChange] = Tileset.AVATAR;
            load.playerX += xChange;
            load.playerY += yChange;

            // Hidden Vision Logic
            if (load.visionHidden) {
                // Set the player tile
                playerVision[load.playerX][load.playerY] = load.underPlayerTile;

                // Look around the player at distance of 3 and try and render what's nearby
                if (load.underPlayerTile.description().equals(Tileset.GRASS.description()) || load.underPlayerTile.description().equals(Tileset.FLOWER.description())) {
                    // player is outside
                    for (int x = load.playerX - 8; x <= load.playerX + 8; x++) {
                        for (int y = load.playerY - 8; y <= load.playerY + 8; y++) {
                            if (x == load.playerX && y == load.playerY) continue;
                            if (isValid(x, y, 0) && !tiles[x][y].description().equals(Tileset.AVATAR.description()) && !tiles[x][y].description().equals(Tileset.FLOOR.description())) {
                                if (!tiles[x][y].description().equals(Tileset.WALL.description()) || (tiles[x][y].description().equals(Tileset.WALL.description())) && (x - load.playerX > -7 && x - load.playerX < 7) && (y - load.playerY > -7 && y - load.playerY < 7)) {
                                    playerVision[x][y] = tiles[x][y];
                                    load.hasVisited[x][y] = true;
                                }
                            }
                        }
                    }
                } else {
                    // player is inside
                    for (int x = load.playerX - 6; x <= load.playerX + 6; x++) {
                        for (int y = load.playerY - 6; y <= load.playerY + 6; y++) {
                            if (x == load.playerX && y == load.playerY) continue;
                            if (isValid(x, y, 0) && !(tiles[x][y].description().equals(Tileset.AVATAR.description()) || tiles[x][y].description().equals(Tileset.GRASS.description()) || tiles[x][y].description().equals(Tileset.FLOWER.description()))) {
                                playerVision[x][y] = tiles[x][y];
                                load.hasVisited[x][y] = true;
                            }
                        }
                    }
                }
                playerVision[load.playerX][load.playerY] = Tileset.AVATAR;
            }

            if (load.underPlayerTile.description().equals(Tileset.GRASS.description()) || load.underPlayerTile.description().equals(Tileset.FLOWER.description())) {
                load.radiation += RandomUtils.uniform(load.random, 4);
                if (load.radiation >= 50) {
                    load.health -= ((load.radiation < 90) ? 0.4 : 0.9) * load.radiation / 25;
                }
            } else {
                load.radiation -= RandomUtils.uniform(load.random, 2);
                if (load.radiation < 50) {
                    load.health += RandomUtils.uniform(load.random, 1) + 1;
                }
            }

            load.radiation = Math.min(100, load.radiation);
            load.radiation = Math.max(0, load.radiation);
            load.health = Math.min(100, load.health);
            load.health = Math.max(0, load.health);

        }
    }

    public void renderMouseHUD(int mouseX, int mouseY, TETile[][] playerVision) {
        TETile tile = load.tiles[mouseX][mouseY];
        StdDraw.setPenColor(Color.WHITE);
        Font oldFont = StdDraw.getFont();
        StdDraw.setFont(fontSmall);
        StdDraw.textRight(WIDTH - 5, 2, load.visionHidden ? playerVision[mouseX][mouseY].description().toUpperCase().equals(Tileset.NOTHING.description().toUpperCase()) ? "UNEXPLORED" : playerVision[mouseX][mouseY].description().toUpperCase() : tile.description().toUpperCase());

        // Return font back to normal to prevent breaking the tiles
        StdDraw.setFont(oldFont);

        load.pMouseX = mouseX;
        load.pMouseY = mouseY;
    }

    public void renderHUD() {
        StdDraw.setPenColor(Color.WHITE);
        Font oldFont = StdDraw.getFont();
        StdDraw.setFont(fontSmall);
        StdDraw.text(8, 2, (load.underPlayerTile.description().equals(Tileset.GRASS.description()) || load.underPlayerTile.description().equals(Tileset.FLOWER.description())) ? "OUTSIDE" : "INSIDE");
        StdDraw.setPenColor((load.radiation > 50) ? Color.red : Color.green);
        StdDraw.textLeft(20, 2, "RADIATION: " + load.radiation);
        StdDraw.setPenColor((load.health > 50) ? Color.green : Color.red);

        StdDraw.textLeft(40, 2, "HEALTH: ");
        StdDraw.rectangle(60, 2.5, 10, 1.5);
        StdDraw.filledRectangle(50 + load.health / 10, 2.5, load.health / 10, 1.5);

        // Return font back to normal to prevent breaking the tiles
        StdDraw.setFont(oldFont);
        StdDraw.show();
    }

    public boolean isValid(int x, int y, int deviation) {
        return (x >= 0 && x <= WIDTH && y >= 0 && y < HEIGHT && (x + deviation) >= 0 && (x - deviation) >= 0 && (y - deviation) >= 0 && (y + deviation) >= 0 && (x + deviation) < WIDTH && (x - deviation < WIDTH) && (y + deviation < HEIGHT) && (y - deviation) < HEIGHT);
    }

    public void displayWin() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.GREEN);

        StdDraw.setFont(fontBig);
        StdDraw.text(WIDTH / 2, HEIGHT / 2 + 4, "YOU WON");

        StdDraw.setFont(fontSmall);
        StdDraw.text(WIDTH / 2, HEIGHT / 2, "You defeated all the bugs!");

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(WIDTH / 2, 5, "Type :q to quit or 'M' to show the main menu.");

        StdDraw.show();
        load = new LoadGame();
    }


    public void displayDeath() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.RED);

        StdDraw.setFont(fontBig);
        StdDraw.text(WIDTH / 2, HEIGHT / 2 + 4, "YOU DIED");

        StdDraw.setFont(fontSmall);
        StdDraw.text(WIDTH / 2, HEIGHT / 2, "I hear data science is fun...");

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(WIDTH / 2, 5, "Type :q to quit or 'M' to show the main menu.");

        StdDraw.show();
        load = new LoadGame();
    }

    public void quitGame() {
        // save
        load.saveGame();
    }

    public void renderMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(fontBig);
        StdDraw.text(WIDTH / 2, HEIGHT / 1.3, "The Quest for a 3.3");

        StdDraw.setFont(fontSmall);
        StdDraw.text(WIDTH / 2, HEIGHT / 2, "New Game (N)");
        StdDraw.text(WIDTH / 2, HEIGHT / 2 - 3, "Load Game (L)");
        StdDraw.text(WIDTH / 2, HEIGHT / 2 - 6, "Quit (Q)");

        StdDraw.show();
    }

    public void renderAwaitSeed(String seed) {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        StdDraw.setFont(fontBig);
        StdDraw.text(WIDTH / 2, HEIGHT / 1.3, "The Quest for a 3.3");

        StdDraw.setFont(fontSmall);
        StdDraw.text(WIDTH / 2, HEIGHT / 2, "Enter a seed:");
        StdDraw.text(WIDTH / 2, HEIGHT / 2 - 3, seed);

        StdDraw.show();
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, both of these calls:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {

        TETile[][] finalWorldFrame = new TETile[WIDTH][HEIGHT];
        input = input.toUpperCase();
        if (!load.isActive && !(input.charAt(0) == 'L')) {
            load = new LoadGame();
            long seed = Long.parseLong(input.substring(1, input.indexOf("S")));
            load.random = new Random(seed);
        }

        // Check first command
        // TODO: ensure length of string is > 0
        char command = input.charAt(0);
        if (command == 'n' || command == 'N') {
            // Create a new world

            // 1. Render a black background
            for (int x = 0; x < WIDTH; x += 1) {
                for (int y = 0; y < HEIGHT; y += 1) {
                    finalWorldFrame[x][y] = Tileset.GRASS;
                    if (RandomUtils.uniform(load.random, 150) < 0.1) {
                        finalWorldFrame[x][y] = Tileset.FLOWER;
                    }
                }
            }

            // Render the game
            createUniverse(finalWorldFrame);
            load.tiles = finalWorldFrame;
            load.visionHidden = false;
            load.isActive = true;

            load.hasVisited = new boolean[WIDTH][HEIGHT];
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    load.hasVisited[x][y] = false;
                }
            }
            playerVision = new TETile[WIDTH][HEIGHT];
                for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    playerVision[x][y] = Tileset.NOTHING;

                    if (load.hasVisited[x][y]) {
                        playerVision[x][y] = load.tiles[x][y];
                    }
                }
            }
            runInputString(input.substring(input.indexOf("S")), finalWorldFrame);
        } else if (command == 'L') {
            // Load game
            load = LoadGame.loadGame();
            if (load == null || !load.isActive) {
                return null;
            }
            playerVision = new TETile[WIDTH][HEIGHT];
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    playerVision[x][y] = Tileset.NOTHING;

                    if (load.hasVisited[x][y]) {
                        playerVision[x][y] = load.tiles[x][y];
                    }
                }
            }
            runInputString(input.substring(1), load.tiles);
        }

        return load.tiles;
    }

    private boolean awaitingQuit = false;
    public void runInputString(String chars, TETile[][] tiles) {
        if (chars.length() == 0) return;
        char c = chars.toUpperCase().charAt(0);
        if (c == ':') {
            awaitingQuit = true;
            runInputString(chars.substring(1), tiles);
        } else if (awaitingQuit && c == 'Q') {
            quitGame();
        } else {
            doMovement(c, tiles);
            awaitingQuit = false;
            runInputString(chars.substring(1), tiles);
        }
    }

    /**
     * Renders a room at position (x, y) with given percentages corresponding to exits, difficulty, and if the room is locked.
     * @param x: x position of the entrance to the room
     * @param y: y position of the entrance to the room
     * @param minArea: minimum size of the room's <b>WALKABLE FLOOR AREA</b>
     * @param exitPercentage: double (0->1) that the room has a guaranteed exit
     * @param isLockedPercentage: double (0->1) that the room is locked
     * @param roomDifficulty: double (0->1) of the room's difficulty
     * @param tiles: reference to the universe
     */
    public void renderRoom(int x, int y, int roomDirection, boolean displayEntrance, int minArea, double exitPercentage, double isLockedPercentage, double roomDifficulty, TETile[][] tiles) {
        // TODO: vary the location of the entrance to the room by one or two tiles up/down/left/right and SIZE 1||2 width

        // 1. Creates dimensions of the room in (sqrt(minArea) + random(1, 5))^2 area
        // NOTE: (+1) allocates additional space for the WALLS of the room
        int width = (int) Math.sqrt(minArea) + RandomUtils.uniform(load.random, 1, 8) + 1;
        int height = (int) Math.sqrt(minArea) + RandomUtils.uniform(load.random, 1, 8) + 1;

        // Check for room collision with border (render end of hallway if so)
        if (x - width < 0 || y - height < 0 || x + width >= WIDTH || y + height >= HEIGHT) {
            return;
        };

        // Modify the direction of the room based on roomDirection
        int xDir = 0;
        int yDir = 0;
        if (roomDirection == 0 || roomDirection == 2) {
            xDir = width / 2;
        } else if (roomDirection == 3) {
            xDir = width - 1;
        }
        if (roomDirection == 1 || roomDirection == 3) {
            yDir = height / 2;
        } else if (roomDirection == 2) {
            yDir = height - 1;
        }

        // Check for no collisions FOR FLOOR AREA (walls CAN overlap)
        for (int i = x + 1; i < width + x - 1; i++) {
            for (int j = y + 1; j < height + y - 1; j++) {
                if (!tiles[i - xDir][j - yDir].description().equals(Tileset.GRASS.description()) && !tiles[i - xDir][j - yDir].description().equals(Tileset.FLOWER.description())) {
                    if (!(i - x > 4 || j - y > 4)) {
                        return;
                    };

                    int max = Math.min(i - x, j - y) + 14;
                    renderHallway(x, y, roomDirection, 1, max, max,false, roomDifficulty, true, tiles);
                    return;
                };
            }
        }

        // Render the initial room with walls with direction modified by roomDirection
        for (int i = x; i < width + x; i++) {
            for (int j = y; j < height + y; j++) {
                // Draws WALLS if edge ELSE draws FLOOR
                if (j == y || j == height + y - 1 || i == x || i == width + x - 1)
                    if (RandomUtils.uniform(load.random, 100) < 3 && (j != y && i != width && j != height + y - 1 && i != width + x - 1))
                        tiles[i - xDir][j - yDir] = Tileset.LOCKED_DOOR;
                    else
                        tiles[i - xDir][j - yDir] = Tileset.WALL;
                else
                    tiles[i - xDir][j - yDir] = Tileset.FLOOR;
            }
        }

        if (RandomUtils.uniform(load.random) < roomDifficulty / 2) {
            // Render an enemy in the room
            load.enemyCount++;
            tiles[x + RandomUtils.uniform(load.random, width/2) + 1 - xDir][y + RandomUtils.uniform(load.random, height/2) + 1 - yDir] = Tileset.TREE;
        }

        // 2. Open the entrance tile (since we most likely created a wall over an existing structure
        if (displayEntrance) {
            // Creates an OPEN entrance if UNLOCKED and a CLOSED, LOCKED entrance if locked
            if (RandomUtils.uniform(load.random) < isLockedPercentage)
                tiles[x][y] = Tileset.LOCKED_DOOR;
            else
                tiles[x][y] = Tileset.FLOOR;
        }

        // 3. Create exit(s) dictated by the exitPercentage
        HashSet<Integer> walls = new HashSet<Integer>();
        if (displayEntrance) {
            walls.add((roomDirection + 2) % 4); // adds the wall opposite to the roomDirection to exempt list (side with entrance)
        }
        while (exitPercentage > 0 && walls.size() < 4) {
            double odds = RandomUtils.uniform(load.random);
            if (odds < exitPercentage) {
                // Create and render an exit
                // 1. Choose a wall that is NOT contained in active wall exits
                int wall = (roomDirection + 2) % 4;
                while (walls.contains(wall)) {
                    wall = RandomUtils.uniform(load.random, 4); // 0, 1, 2, 3
                }

                walls.add(wall);

                int wallX, wallY;
                // TODO: vary the entrance position
                if (wall == 0) {
                    wallX = x + width / 2 - xDir; // ANY
                    wallY = height + y - yDir;
                } else if (wall == 1) {
                    wallX = width + x - xDir - 1;
                    wallY = height / 2 + y - yDir; // ANY
                } else if (wall == 2) {
                    wallX = x + width / 2 - xDir; // ANY
                    wallY = y - yDir - 1;
                } else{
                    wallX = x - xDir;
                    wallY = height / 2 + y - yDir; // ANY
                }

                renderHallway(wallX, wallY, wall, 1, 3 + RandomUtils.uniform(load.random, 7), 15, true, roomDifficulty, false, tiles);
            }
            exitPercentage -= odds / 4;
        }
    }

    public void renderHallway(int x, int y, int direction, int width, int minLength, int maxLength, boolean hasEndRoom, double connectingDifficulty, boolean overlaps, TETile[][] tiles) {

        int length = Math.min(minLength + RandomUtils.uniform(load.random, 5), maxLength);

        if (x - length < 4 || y - length < 4 || x + width >= WIDTH || y + length + 4 >= HEIGHT || x + length >= WIDTH || y + 4 + width >= HEIGHT) return;

        if (RandomUtils.uniform(load.random, 5) < 1 && direction == 0) {
            renderHallway(x + 1, y+3, 1, 1, 8, 10, false, connectingDifficulty, true, tiles);
        } else if (RandomUtils.uniform(load.random, 5) < 1 && direction == 1) {
            renderHallway(x + 3, y+1, 0, 1, 8, 10, false, connectingDifficulty, true, tiles);
        } else if (RandomUtils.uniform(load.random, 5) < 1 && direction == 2) {
            renderHallway(x - 1, y+3, 3, 1, 8, 10, false, connectingDifficulty, true, tiles);
        } else if (RandomUtils.uniform(load.random, 5) < 1 && direction == 3) {
            renderHallway(x + 3, y-1, 2, 1, 8, 10, false, connectingDifficulty, true, tiles);
        }

        // Swap the direction of the hallway depending on direction
        int xDir = 0;
        int yDir = 0;

        if (direction == 0) {
            yDir -= 2;
            xDir += 1;
            y -= 1;
        }
        if (direction == 2) {
            yDir = length + 1;
            xDir += 1;
            y += 1;
        }
        if (direction == 3) xDir = length;

        boolean swap = false;
        if (direction == 0 || direction == 2) {
            swap = true;
            int temp = length;
            length = width + 2;
            width = temp;
        }

        for (int i = x + 1; i < x + length - 1; i++) {
            for (int j = y; j < y + width + 2; j++) {
                // Renders the walls and floors (and swaps them if it's a vertical hallway)
                if (!tiles[i - xDir][j - 1 - yDir].description().equals(Tileset.GRASS.description()) && !tiles[i - xDir][j - 1 - yDir].description().equals(Tileset.FLOWER.description()) && !overlaps) {
                    // Collision
                    if (swap) {
                        width = j - y;
                    } else {
                        length = i - x;
                    }
                    break;
                }
            }
        }

        for (int i = x; i < x + length; i++) {
            for (int j = y; j < y + width + 2; j++) {
                // Renders the walls and floors (and swaps them if it's a vertical hallway
                if (!tiles[i - xDir][j - 1 - yDir].description().equals(Tileset.TREE.description()) && !tiles[i - xDir][j - 1 - yDir].description().equals(Tileset.FLOOR.description()) && ((swap && (i == x || i == x + length - 1)) || (!swap && (j == y || j == y + width + 1)))) {
                    tiles[i - xDir][j - 1 - yDir] = Tileset.WALL;
                }
                else if (!tiles[i - xDir][j - 1 - yDir].description().equals(Tileset.TREE.description()))
                    tiles[i - xDir][j - 1 - yDir] = Tileset.FLOOR;
            }
        }

        if (!(length < 2 || width < 1 || !hasEndRoom)) {
            if (direction == 0) {
                renderRoom(x, y + width + 2, 0, true, 16, 0.8, 0.5, connectingDifficulty + RandomUtils.uniform(load.random)/3, tiles);
            } else if (direction == 1) {
                renderRoom(x + length - 1, y, 1, true, 16, 0.8, 0.5, connectingDifficulty + RandomUtils.uniform(load.random)/3, tiles);
            } else if (direction == 2) {
                renderRoom(x, y - width - 2, 2, true, 16, 0.8, 0.5, connectingDifficulty + RandomUtils.uniform(load.random)/3, tiles);
            } else if (direction == 3) {
                renderRoom(x - length - 1, y, 3, true, 16, 0.8, 0.5, connectingDifficulty + RandomUtils.uniform(load.random)/3, tiles);
            }
        };

        tiles[x][y] = Tileset.FLOOR;
        if (direction == 0) {
            if (!tiles[x + 1][y + width + 4].description().equals(Tileset.GRASS.description()) && !tiles[x + 2][y + width + 3].description().equals(Tileset.GRASS.description()) && tiles[x + 1][y + width + 3] == Tileset.WALL && tiles[x][y + width + 3] != Tileset.LOCKED_DOOR)
                tiles[x + 1][y + width + 3] = Tileset.FLOOR;
            if (!tiles[x - 1][y + width + 4].description().equals(Tileset.GRASS.description()) && !tiles[x - 2][y + width + 3].description().equals(Tileset.GRASS.description()) && tiles[x - 1][y + width + 3] == Tileset.WALL && tiles[x][y + width + 3] != Tileset.LOCKED_DOOR)
                tiles[x - 1][y + width + 3] = Tileset.FLOOR;
            if (tiles[x][y + width + 3].description().equals(Tileset.GRASS.description())) {
                tiles[x][y + width + 3] = Tileset.WALL;
                tiles[x + 1][y + width + 3] = Tileset.WALL;
                tiles[x - 1][y + width + 3] = Tileset.WALL;
            }
        } else if (direction == 1) {
            if (tiles[x + length][y] == Tileset.WALL)
                tiles[x + length][y] = Tileset.FLOOR;
            if (tiles[x + length][y].description().equals(Tileset.GRASS.description()) || tiles[x + length][y].description().equals(Tileset.FLOWER.description())) {
                tiles[x + length][y] = Tileset.WALL;
                tiles[x + length][y + 1] = Tileset.WALL;
                tiles[x + length][y - 1] = Tileset.WALL;
            }
        } else if (direction == 2) {
            if (tiles[x][y - width - 3] == Tileset.WALL)
                tiles[x][y - width - 3] = Tileset.FLOOR;
            if (tiles[x][y - width - 3].description().equals(Tileset.GRASS.description()) || tiles[x][y - width - 3].description().equals(Tileset.FLOWER.description())) {
                tiles[x][y - width - 3] = Tileset.WALL;
                tiles[x + 1][y - width - 3] = Tileset.WALL;
                tiles[x - 1][y - width - 3] = Tileset.WALL;
            }
        } else if (direction == 3) {
            if (tiles[x - length - 1][y].description().equals(Tileset.GRASS.description()) || tiles[x - length - 1][y].description().equals(Tileset.FLOWER.description())) {
                tiles[x - length - 1][y] = Tileset.WALL;
                tiles[x - length - 1][y + 1] = Tileset.WALL;
                tiles[x - length - 1][y - 1] = Tileset.WALL;
            }
        }

    }

    public void createUniverse(TETile[][] tiles) {
        int roomGenX = WIDTH / 2 + RandomUtils.uniform(load.random, -10, 10);
        int roomGenY = HEIGHT / 2 + RandomUtils.uniform(load.random, -4, 4);

        // Render an initial unlocked room with an exit and zero difficulty near the center of the world
        renderRoom(roomGenX, roomGenY, 1, false, 16, 30, 0.0, 0.0, tiles);

        // Render the player
        load.playerX = roomGenX + 2;
        load.playerY = roomGenY + 2;
        tiles[load.playerX][load.playerY] = Tileset.AVATAR;
        load.underPlayerTile = Tileset.FLOOR;
    }
}
