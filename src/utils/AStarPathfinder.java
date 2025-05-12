package utils;
import mapGrid.MapGrid;
import mapGrid.GridCell;
import java.util.*;

public class AStarPathfinder {

    static class Node implements Comparable<Node> {
        int x, y;
        double g, h;
        Node parent;

        Node(int x, int y, double g, double h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        double f() {
            return g + h;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f(), other.f());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) return false;
            Node other = (Node) obj;
            return this.x == other.x && this.y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static List<int[]> findPath(MapGrid map, int startX, int startY, int goalX, int goalY) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Set<Node> closed = new HashSet<>();

        Node start = new Node(startX, startY, 0, heuristic(startX, startY, goalX, goalY), null);
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }

            closed.add(current);

            for (int[] dir : new int[][]{{0,1},{1,0},{0,-1},{-1,0}}) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                // if (!map(nx, ny)) continue;

                GridCell cell = map.getCell(nx, ny);
                if (cell.isOnFire) continue; // optional: firefighter can't walk into fire

                Node neighbor = new Node(nx, ny, current.g + 1, heuristic(nx, ny, goalX, goalY), current);
                if (closed.contains(neighbor)) continue;

                open.add(neighbor);
            }
        }

        return new ArrayList<>(); // no path found
    }

    private static double heuristic(int x1, int y1, int x2, int y2) {
        // Manhattan distance
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static List<int[]> reconstructPath(Node node) {
        List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(0, new int[]{node.x, node.y});
            node = node.parent;
        }
        return path;
    }
}