import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates assets/modernconfig/icon.png: the mod icon, a plain white
 * cogwheel on a transparent background. The gear is the Lucide "settings"
 * icon, the same glyph the Modern GUI library embeds for in-game settings
 * buttons (https://lucide.dev, ISC license), so the mod icon and the UI
 * iconography match. Regenerate offline with:
 *
 * <pre>   java tools/gen-icon.java [out.png]</pre>
 *
 * <p>The embedded parser is the same mini-parser as the Modern GUI
 * generator: it covers the SVG path subset Lucide uses (M/L/H/V/C/S/Q/T/A/Z
 * with relative forms and implicit repeats, plus circle and polygon
 * elements). The 24-unit icon is rasterised at 512 px with anti-aliasing
 * and downscaled to 128 in two steps for smooth strokes.
 */
public class GenIcon {

    /** Lucide "settings": gear outline + inner circle, on the 24-unit grid. */
    static final String[] SETTINGS = {
            "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
            "circle:12 12 3",
    };

    static final int SIZE = 128;
    static final int OVER = 512; // supersample canvas
    static final double SCALE = OVER / 24.0;

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(OVER, OVER, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        // Stroke width is in user-space units and the transform scales by SCALE,
        // so this is Lucide's stroke-width 2 on the 24-unit grid.
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.translate(OVER / 2.0, OVER / 2.0);
        g.scale(SCALE, SCALE);
        g.translate(-12, -12); // centre the 24-unit grid
        for (String element : SETTINGS) {
            g.draw(shape(element));
        }
        g.dispose();

        // Two-step halving keeps the strokes smooth at 128 px.
        BufferedImage half = scale(img, OVER / 2);
        BufferedImage out = scale(half, SIZE);

        File file = new File(args.length > 0 ? args[0]
                : "src/main/resources/assets/modernconfig/icon.png");
        ImageIO.write(out, "png", file);
        System.out.println("wrote " + file.getAbsolutePath() + " (" + file.length()
                + " bytes, " + SIZE + "x" + SIZE + ")");
    }

    static BufferedImage scale(BufferedImage src, int size) {
        BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return dst;
    }

    static Path2D shape(String element) {
        if (element.startsWith("circle:")) {
            String[] p = element.substring(7).trim().split("\\s+");
            double r = Double.parseDouble(p[2]);
            Path2D path = new Path2D.Float();
            path.append(new Ellipse2D.Double(Double.parseDouble(p[0]) - r,
                    Double.parseDouble(p[1]) - r, 2 * r, 2 * r), false);
            return path;
        }
        if (element.startsWith("polygon:")) {
            double[] pts = doubles(element.substring(8));
            Path2D path = new Path2D.Float();
            path.moveTo(pts[0], pts[1]);
            for (int i = 2; i < pts.length; i += 2) {
                path.lineTo(pts[i], pts[i + 1]);
            }
            path.closePath();
            return path;
        }
        return parse(element);
    }

    // ------------------------------------------------------------------ path parser
    // Same parser as Modern GUI's tools/gen-icons.java (M/L/H/V/C/S/Q/T/A/Z with
    // relative forms and implicit repeats); kept verbatim so both mods rasterise
    // identically.

    static final Pattern TOKEN = Pattern.compile(
            "[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

    static Path2D parse(String d) {
        List<String> t = new ArrayList<>();
        Matcher m = TOKEN.matcher(d);
        while (m.find()) {
            t.add(m.group());
        }
        Path2D p = new Path2D.Float();
        double cx = 0, cy = 0, sx = 0, sy = 0;
        double px = 0, py = 0; // previous cubic control, for S
        double qx = 0, qy = 0; // previous quad control, for T
        char prev = 0;
        int i = 0;
        while (i < t.size()) {
            String tk = t.get(i);
            char cmd;
            if (Character.isLetter(tk.charAt(0))) {
                cmd = tk.charAt(0);
                i++;
            } else if (prev == 'M' || prev == 'm') {
                cmd = (char) (prev - 1); // implicit lineto after moveto, same relative flag
            } else {
                cmd = prev;
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    double x = n(t, i++), y = n(t, i++);
                    if (rel) { x += cx; y += cy; }
                    p.moveTo(x, y);
                    cx = sx = x; cy = sy = y;
                }
                case 'L' -> {
                    double x = n(t, i++), y = n(t, i++);
                    if (rel) { x += cx; y += cy; }
                    p.lineTo(x, y);
                    cx = x; cy = y;
                }
                case 'H' -> {
                    double x = n(t, i++);
                    if (rel) { x += cx; }
                    p.lineTo(x, cy);
                    cx = x;
                }
                case 'V' -> {
                    double y = n(t, i++);
                    if (rel) { y += cy; }
                    p.lineTo(cx, y);
                    cy = y;
                }
                case 'C' -> {
                    double x1 = n(t, i++), y1 = n(t, i++), x2 = n(t, i++), y2 = n(t, i++),
                            x = n(t, i++), y = n(t, i++);
                    if (rel) { x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy; }
                    p.curveTo(x1, y1, x2, y2, x, y);
                    px = x2; py = y2; cx = x; cy = y;
                }
                case 'S' -> {
                    double x1 = 2 * cx - px, y1 = 2 * cy - py;
                    double x2 = n(t, i++), y2 = n(t, i++), x = n(t, i++), y = n(t, i++);
                    if (rel) { x2 += cx; y2 += cy; x += cx; y += cy; }
                    p.curveTo(x1, y1, x2, y2, x, y);
                    px = x2; py = y2; cx = x; cy = y;
                }
                case 'Q' -> {
                    double x1 = n(t, i++), y1 = n(t, i++), x = n(t, i++), y = n(t, i++);
                    if (rel) { x1 += cx; y1 += cy; x += cx; y += cy; }
                    p.quadTo(x1, y1, x, y);
                    qx = x1; qy = y1; cx = x; cy = y;
                }
                case 'T' -> {
                    double x1 = 2 * cx - qx, y1 = 2 * cy - qy;
                    double x = n(t, i++), y = n(t, i++);
                    if (rel) { x += cx; y += cy; }
                    p.quadTo(x1, y1, x, y);
                    qx = x1; qy = y1; cx = x; cy = y;
                }
                case 'A' -> {
                    double rx = n(t, i++), ry = n(t, i++), rot = n(t, i++);
                    boolean large = n(t, i++) > 0.5, sweep = n(t, i++) > 0.5;
                    double x = n(t, i++), y = n(t, i++);
                    if (rel) { x += cx; y += cy; }
                    arcTo(p, cx, cy, rx, ry, rot, large, sweep, x, y);
                    cx = x; cy = y;
                }
                case 'Z' -> {
                    p.closePath();
                    cx = sx; cy = sy;
                }
                default -> throw new IllegalArgumentException("unsupported path command: " + cmd);
            }
            prev = cmd;
        }
        return p;
    }

    static double n(List<String> t, int i) {
        return Double.parseDouble(t.get(i));
    }

    static double[] doubles(String s) {
        Matcher m = TOKEN.matcher(s);
        List<Double> vals = new ArrayList<>();
        while (m.find()) {
            vals.add(Double.parseDouble(m.group()));
        }
        double[] out = new double[vals.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = vals.get(i);
        }
        return out;
    }

    /**
     * SVG arc to cubic beziers: endpoint-to-centre parameterisation from the SVG
     * specification (appendix F.6), split into <=90 degree segments so the curve
     * stays exact.
     */
    static void arcTo(Path2D p, double x0, double y0, double rx, double ry, double rotDeg,
                      boolean largeArc, boolean sweep, double x1, double y1) {
        if (x0 == x1 && y0 == y1 || rx <= 0 || ry <= 0) {
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotDeg % 360.0);
        double cosP = Math.cos(phi), sinP = Math.sin(phi);
        double dx = (x0 - x1) / 2.0, dy = (y0 - y1) / 2.0;
        double x1p = cosP * dx + sinP * dy;
        double y1p = -sinP * dx + cosP * dy;

        double rx2 = rx * rx, ry2 = ry * ry;
        double lambda = x1p * x1p / rx2 + y1p * y1p / ry2;
        if (lambda > 1.0) {
            double s = Math.sqrt(lambda);
            rx *= s; ry *= s;
            rx2 = rx * rx; ry2 = ry * ry;
        }

        double sign = largeArc != sweep ? 1.0 : -1.0;
        double numer = rx2 * ry2 - rx2 * y1p * y1p - ry2 * x1p * x1p;
        double denom = rx2 * y1p * y1p + ry2 * x1p * x1p;
        double co = sign * Math.sqrt(Math.max(0.0, numer / denom));
        double cxp = co * rx * y1p / ry;
        double cyp = -co * ry * x1p / rx;
        double ccx = cosP * cxp - sinP * cyp + (x0 + x1) / 2.0;
        double ccy = sinP * cxp + cosP * cyp + (y0 + y1) / 2.0;

        double ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry;
        double theta1 = angle(1.0, 0.0, ux, uy);
        double delta = angle(ux, uy, vx, vy);
        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }

        int segments = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 2.0)));
        double step = delta / segments;
        double k = 4.0 / 3.0 * Math.tan(step / 4.0);
        double th = theta1;
        for (int i = 0; i < segments; i++) {
            double th2 = th + step;
            double[] a = point(ccx, ccy, rx, ry, cosP, sinP, th);
            double[] b = point(ccx, ccy, rx, ry, cosP, sinP, th2);
            double adx = -rx * cosP * Math.sin(th) - ry * sinP * Math.cos(th);
            double ady = -rx * sinP * Math.sin(th) + ry * cosP * Math.cos(th);
            double bdx = -rx * cosP * Math.sin(th2) - ry * sinP * Math.cos(th2);
            double bdy = -rx * sinP * Math.sin(th2) + ry * cosP * Math.cos(th2);
            p.curveTo(a[0] + k * adx, a[1] + k * ady, b[0] - k * bdx, b[1] - k * bdy, b[0], b[1]);
            th = th2;
        }
    }

    static double[] point(double ccx, double ccy, double rx, double ry, double cosP, double sinP,
                          double th) {
        double ex = rx * Math.cos(th), ey = ry * Math.sin(th);
        return new double[]{ccx + cosP * ex - sinP * ey, ccy + sinP * ex + cosP * ey};
    }

    static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        double a = Math.acos(Math.max(-1.0, Math.min(1.0, dot / (len == 0 ? 1 : len))));
        return (ux * vy - uy * vx) < 0 ? -a : a;
    }
}
