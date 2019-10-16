import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */

public class Rasterer {

    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;

    public static final double ROOT_LONDPP = (ROOT_LRLON - ROOT_ULLON) / 256;

    public Rasterer() {
        // YOUR CODE HERE
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        Map<String, Object> results = new HashMap<>();

        double lRightLong = params.get("lrlon");
        double lRightLat = params.get("lrlat");
        double upLeftLong = params.get("ullon");
        double upLeftLat = params.get("ullat");
        double width = params.get("w");
        int depth = 0;
        double rootLonDPP = (ROOT_LRLON - ROOT_ULLON) / 256;
        double queryLonDPP = (lRightLong - upLeftLong) / width;
        double rasterULLONCorner;
        double rasterULLATCorner;
        double rasterLRLONCorner;
        double rasterLRLATCorner;

        if (lRightLong <= ROOT_ULLON || lRightLat >= ROOT_ULLAT || upLeftLong >= ROOT_LRLON
                || upLeftLat <= ROOT_LRLAT || lRightLong <= upLeftLong || lRightLat >= upLeftLat) {
            results.put("render_grid", "");
            results.put("raster_ul_lon", "");
            results.put("raster_ul_lat", "");
            results.put("raster_lr_lon", "");
            results.put("raster_lr_lat", "");
            results.put("depth", "");
            results.put("query_success", false);
            return results;
        } else {
            for (double i = queryLonDPP; i < rootLonDPP; rootLonDPP = rootLonDPP / 2) {
                depth += 1;
                if (depth == 7) {
                    break;
                }
            }

            double tileWidthLong = Math.abs(ROOT_ULLON - ROOT_LRLON) / Math.pow(2, depth);
            double tileHeightLAT = Math.abs(ROOT_ULLAT - ROOT_LRLAT) / Math.pow(2, depth);

            int[] xIndexRange = getULLONIndex(upLeftLong, lRightLong, depth);
            int[] yIndexRange = getLRLONIndex(upLeftLong, upLeftLat, lRightLat, depth);

            rasterULLONCorner = ROOT_ULLON + (xIndexRange[0] * tileWidthLong);
            rasterULLATCorner = ROOT_ULLAT - (yIndexRange[0] * tileHeightLAT);
            rasterLRLONCorner = ROOT_ULLON + (xIndexRange[1] + 1) * tileWidthLong;
            rasterLRLATCorner = ROOT_ULLAT - (yIndexRange[1] + 1) * tileHeightLAT;

            int numOfCols = xIndexRange[1] - xIndexRange[0] + 1;
            int numOfRows = yIndexRange[1] - yIndexRange[0] + 1;

            String[][] images = new String[numOfRows][numOfCols];
            if (depth == 0) {
                images[0][0] = "d0_x0_y0";
            } else {
                for (int y = yIndexRange[0]; y <= yIndexRange[1]; y++) {

                    for (int x = xIndexRange[0]; x <= xIndexRange[1]; x++) {
                        String dString = Integer.toString(depth);
                        String xString = Integer.toString(x);
                        String yString = Integer.toString(y);
                        int row = y - yIndexRange[0];
                        int col = x - xIndexRange[0];
                        images[row][col] = "d" + dString + "_x" + xString + "_y" + yString + ".png";
                    }
                }
            }

            results.put("render_grid", images);
            results.put("raster_ul_lon", rasterULLONCorner);
            results.put("raster_ul_lat", rasterULLATCorner);
            results.put("raster_lr_lon", rasterLRLONCorner);
            results.put("raster_lr_lat", rasterLRLATCorner);
            results.put("depth", depth);
            results.put("query_success", true);
            return results;
        }
    }

    public int[] getLRLONIndex(double upLeftLong, double upLeftLat, double lRightLat, int depth) {
        int indexTop = 0;
        int indexBot = (int) Math.pow(2, depth) - 1;
        double tileHeightLAT = Math.abs(ROOT_ULLAT - ROOT_LRLAT) / Math.pow(2, depth);
        int[] yIndexRange = new int[2];

        if (upLeftLat > ROOT_ULLAT && upLeftLong < (ROOT_ULLAT + tileHeightLAT)) {
            yIndexRange[0] = 0;
        } else {
            for (double i = ROOT_ULLAT; i > upLeftLat; i -= tileHeightLAT) {
                if (i - tileHeightLAT < upLeftLat) {
                    break;
                }
                indexTop += 1;
            }
            yIndexRange[0] = indexTop;
        }

        if (lRightLat > ROOT_LRLAT && lRightLat < (ROOT_LRLAT - tileHeightLAT)) {
            yIndexRange[1] = indexBot;
        } else {
            for (double i = ROOT_LRLAT; i < lRightLat; i += tileHeightLAT) {
                if (i + tileHeightLAT > lRightLat) {
                    break;
                }
                indexBot -= 1;
            }
            yIndexRange[1] = indexBot;
        }
        return yIndexRange;
    }

    public int[] getULLONIndex(double upLeftLon, double lRightLon, int depth) {
        int indexLeft = 0;
        int indexRight = (int) Math.pow(2, depth) - 1;
        double tileWidthLong = Math.abs(ROOT_ULLON - ROOT_LRLON) / Math.pow(2, depth);

        int[] xIndexRange = new int[2];

        if (upLeftLon > ROOT_ULLON && upLeftLon < (ROOT_ULLON + tileWidthLong)) {
            xIndexRange[0] = 0;
        } else {
            for (double i = ROOT_ULLON; i < upLeftLon; i += tileWidthLong) {
                if (i + tileWidthLong > upLeftLon) {
                    break;
                }
                indexLeft += 1;
            }
            xIndexRange[0] = indexLeft;
        }

        if (lRightLon < ROOT_LRLON && lRightLon > (ROOT_LRLON - tileWidthLong)) {
            xIndexRange[1] = indexRight;
        } else {
            for (double i = ROOT_LRLON; i > lRightLon; i -= tileWidthLong) {
                if (i - tileWidthLong < lRightLon) {
                    break;
                }
                indexRight -= 1;
            }
            xIndexRange[1] = indexRight;
        }
        return xIndexRange;
    }
}





