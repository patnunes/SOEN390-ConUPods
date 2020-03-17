package astar;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static pictureMapper.GetPixelColor.getBoolArr;
import static pictureMapper.GetPixelColor.getRGBarray;

public class AStar {

    public static void linkNeighbors(Spot[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < (grid[i].length) - 1; j++) {
                if (i > 0) {
                    grid[i][j].addNeighbor(grid[i - 1][j]);
                }
                if (j > 0) {
                    grid[i][j].addNeighbor(grid[i][j - 1]);
                }
                if (i < grid.length - 1) {
                    grid[i][j].addNeighbor(grid[i + 1][j]);
                }
                if (j < grid.length - 1) {
                    grid[i][j].addNeighbor(grid[i][j + 1]);
                }
                if (i > 0 && j > 0) {
                    grid[i][j].addNeighbor(grid[i - 1][j - 1]);
                }
                if (i < grid.length - 1 && j < grid.length - 1) {
                    grid[i][j].addNeighbor(grid[i + 1][j + 1]);
                }
            }
        }
    }

    public static Edges pointsToArea(JSONObject coords, int gridSize, int ratio) {
        // The min max default values are set
        // respectively to the 25x25 array size
        // generated by the metadata tool
        int minX = gridSize;
        int minY = gridSize;
        int maxX = 0;
        int maxY = 0;

        for (int i = 0; i < coords.size(); i++) {

            JSONObject xy = (JSONObject) coords.get(String.valueOf(i));

            int x = Math.toIntExact((long) xy.get("x"));
            int y = Math.toIntExact((long) xy.get("y"));

            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        Edges square = new Edges(minX, maxX, minY, maxY, ratio);
        return square;
    }

    public static Edges[] getDictFromJSON(String start, String end, String floor, String filePath) throws ParseException, IOException {

        JSONParser parser = new JSONParser();

        //filePath is assumed to have a path to JSON
        //think about exception handling
        JSONObject floorJSON = (JSONObject) parser.parse(new FileReader(filePath));

        JSONObject roomJSON = (JSONObject) floorJSON.get(floor);

        JSONObject startCoords = (JSONObject) roomJSON.get(start);
        JSONObject endCoords = (JSONObject) roomJSON.get(end);

        // gridSize and ratio must be dynamically calculated, since if we change map size, this might cause issue
        int gridSize = 25;
        int ratio = 11;

        Edges startDict = pointsToArea(startCoords, gridSize, ratio);
        Edges endDict = pointsToArea(endCoords, gridSize, ratio);

        Edges[] startEnd = {startDict, endDict};

        return startEnd;
    }

    public static boolean[][] createBinaryGrid(String path) throws IOException {

        int walkable = -534826;

        BufferedImage image = ImageIO.read(new File(path));
        int[][] result = getRGBarray(image);
        boolean[][] bool = getBoolArr(result, walkable);

        return bool;
    }

    public static Spot[][] createSpotGrid(boolean[][] bool, Edges[] startEnd) {

        Spot[][] grid = new Spot[bool.length][bool[bool.length - 1].length];

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (i <= startEnd[0].getRight()
                        && i >= startEnd[0].getLeft()
                        && j <= startEnd[0].getTop()
                        && j >= startEnd[0].getBottom()) {

                    grid[i][j] = new Spot(i, j, false);

                } else if (i <= startEnd[1].getRight()
                        && i >= startEnd[1].getLeft()
                        && j <= startEnd[1].getTop()
                        && j >= startEnd[1].getBottom()) {

                    grid[i][j] = new Spot(i, j, false);

                } else {

                    grid[i][j] = new Spot(i, j, bool[i][j]);

                }
            }
        }

        return grid;
    }

    public static Spot algo(Spot[][] grid, int x1, int y1, int x2, int y2) {

        java.util.List<Spot> openSet = new ArrayList<>();
        List<Spot> closedSet = new ArrayList<>();

        Spot start = grid[x1][y1];
        Spot end = grid[x2][y2];

        if (start.wall) {
            return null;
        }
        if (end.wall) {
            return null;
        }

        start.setH(distance(start, end));
        start.updateF();

        openSet.add(start);

        Spot current;
        Spot path = null;

        while (openSet.size() > 0) {

            //*** PART OF DEBUG DRAWING ***
            //timer is only here for slowly down, to give the GUI time to render each step
//            try {
//                TimeUnit.MILLISECONDS.sleep(50);
//            } catch (Exception e) {
//                continue;
//            }

            //check the node in the openSet with lowest F score
            current = openSet.get(0);

            for (Spot candidate : openSet) {
                if (candidate.getF() == current.getF()) {
                    if (candidate.getG() < current.getG()) {
                        current = candidate;
                    }
                }
                if (candidate.getF() < current.getF()) {
                    current = candidate;
                }
            }

            openSet.remove(current);
            closedSet.add(current);

            //*** PART OF DEBUG DRAWING ***
//            MyDrawable green = new MyDrawable(new Rectangle(current.getX() * w, current.getY() * h, w, h), new Color(0f, 1f, 0f, 0.1f), new BasicStroke(0), true);
//            gui.addMyDrawable(green);

            for (Spot neighbor : current.getNeighbors()) {
                if (!closedSet.contains(neighbor) && !neighbor.wall) {

                    double tent_score = current.getG() + distance(current, neighbor);

                    if (openSet.contains(neighbor)) {
                        if (tent_score < neighbor.getG()) {

                            neighbor.setG(tent_score);

                        }
                    } else {

                        neighbor.setG(tent_score);
                        openSet.add(neighbor);

                        //*** PART OF DEBUG DRAWING ***
//                        MyDrawable red = new MyDrawable(new Rectangle(neighbor.getX() * w, neighbor.getY() * h, w, h), new Color(1f, 0f, 0f, 0.4f), new BasicStroke(0), true);
//                        gui.addMyDrawable(red);
                    }

                    neighbor.setPrevious(current);

                    neighbor.setH(distance(neighbor, end));
                    neighbor.updateF();

                }

            }

            if (current.equals(end)) {

                path = current;
                break;

            }

        }

        return path;

    }

    /*
        The following distance methods are left here for the time being,
        but will be removed in the final iteration. They are left here
        for further testing. So far, Chebyshev distance is the most promising one.

        //Manhattan distance formula
        public static double distance(Spot a, Spot b) {
            return (Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()));
        }

        //Euclidean distance formula
        static public double distance(Spot a, Spot b) {
            int deltaX = a.getX() - b.getX();
            int deltaY = a.getY() - b.getY();
            return ((Math.pow(deltaX, 2) + Math.pow(deltaY, 2)));
        }
    */
    //Chebyshev distance formula
    static public double distance(Spot a, Spot b) {
        int deltaX = a.getX() - b.getX();
        int deltaY = a.getY() - b.getY();
        return ((Math.max(Math.abs(deltaX), Math.abs(deltaY))));
    }


}
