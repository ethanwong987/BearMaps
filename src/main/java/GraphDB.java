
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Map;



/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */

    private HashMap<Long, Node> allNodes = new HashMap<>();
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    void addNewNode(Node nd) {
        allNodes.put(nd.id(), nd);
    }

    Node getNodeFromAll(long id) {
        return allNodes.get(id);
    }


    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        System.out.println("Im being cleaned");
        ArrayList<Node> toDelete = cleanHelper();
        for (Node n : toDelete) {
            allNodes.remove(n.id);
        }
    }

    ArrayList<Node> cleanHelper() {
        ArrayList<Node> delete = new ArrayList<>();
        for (Long key : allNodes.keySet()) {
            Node target = getNodeFromAll(key);
            if (target.adjNodeIds.isEmpty()) {
                delete.add(target);
            }
        }
        return delete;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */

    Iterable<Long> vertices() {
        ArrayList<Long> allNodesKeys = new ArrayList();
        for (Long key : allNodes.keySet()) {
            allNodesKeys.add(key);
        }
        return allNodesKeys;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        ArrayList<Long> adjNodes = new ArrayList();
//        boolean is = allNodes.containsKey(v);
//        System.out.println(is);
        Node targetNode = getNodeFromAll(v);
//        System.out.println(targetNode.id);
//        System.out.println(targetNode.adjNodes());
//        System.out.println("t" + targetNode.id);
        for (Long id : targetNode.adjNodeIds) {
            adjNodes.add(id);
//            System.out.println(adjNodes);
        }
        //System.out.println(adjNodes);
        return adjNodes;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }


    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    static class Node implements Comparable<Node> {
        long id;
        double lon;
        double lat;
        String name;
        Map<String, String> tags;
        ArrayList<Long> adjNodeIds = new ArrayList<>();
        double distFromStartPriority;
        //if adj already in this then don't add.

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.name = "";
            this.tags = null;
        }

        public double getDistFromStartPriority() {
            return distFromStartPriority;
        }

        public long id() {
            return id;
        }

        public double lon() {
            return lon;
        }

        public double lat() {
            return lat;
        }

        public String name() {
            return name;
        }

        public ArrayList adjNodes() {
            return adjNodeIds;
        }

        public int compareTo(Node node) {
            return Double.compare(distFromStartPriority, node.distFromStartPriority);
        }
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        Queue<Double> nodeIdPQ = new PriorityQueue<>();
        HashMap<Double, Long> nodeDistance = new HashMap<>();

        for (Long n : vertices()) {
            Node node = getNodeFromAll(n);
            double distanceFromTarget = distance(node.lon, node.lat, lon, lat);
            nodeIdPQ.add(distanceFromTarget);
            nodeDistance.put(distanceFromTarget, node.id);
        }
        return nodeDistance.get(nodeIdPQ.poll());
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return allNodes.get(v).lon();
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return allNodes.get(v).lat();
    }

}

