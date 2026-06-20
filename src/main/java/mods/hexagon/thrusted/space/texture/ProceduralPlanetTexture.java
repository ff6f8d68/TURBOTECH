package mods.hexagon.thrusted.space.texture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

// import statements above

public class ProceduralPlanetTexture {
    private static final int TEXTURE_SIZE = 256;

    public static BufferedImage generatePlanetTexture(String name, int color, double radius, boolean hasAtmosphere) {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE * 2, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Random rng = new Random(name.hashCode() * 31L + (long) (radius * 100));

        Color baseColor = new Color(color, true);

        if (name.contains("earth") || name.contains("venus")) {
            generateTerrestrialTexture(g, name, baseColor, rng);
        } else if (name.contains("mars")) {
            generateMartianTexture(g, baseColor, rng);
        } else if (name.contains("jupiter") || name.contains("saturn")) {
            generateGasGiantTexture(g, baseColor, rng);
        } else if (name.contains("mercury") || name.contains("moon")) {
            generateRockyTexture(g, baseColor, rng);
        } else {
            generateGenericTexture(g, baseColor, rng);
        }

        if (hasAtmosphere) {
            addAtmosphereEffect(img, g, baseColor, rng);
        }

        g.dispose();
        return img;
    }

    private static void generateTerrestrialTexture(Graphics2D g, String name, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2, h = TEXTURE_SIZE;
        int gridSize = 8 + rng.nextInt(8);
        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                Color cellColor = varyColor(base, rng, 30);
                int x0 = gx * w / gridSize;
                int y0 = gy * h / gridSize;
                int x1 = (gx + 1) * w / gridSize;
                int y1 = (gy + 1) * h / gridSize;
                g.setColor(cellColor);
                g.fillRect(x0, y0, x1 - x0, y1 - y0);
            }
        }
        int detailPasses = 3 + rng.nextInt(3);
        for (int p = 0; p < detailPasses; p++) {
            int subGrid = 4 + rng.nextInt(8);
            for (int gy = 0; gy < subGrid; gy++) {
                for (int gx = 0; gx < subGrid; gx++) {
                    int x0 = gx * w / subGrid;
                    int y0 = gy * h / subGrid;
                    Color blend = varyColor(base, rng, 20);
                    g.setColor(new Color(blend.getRed(), blend.getGreen(), blend.getBlue(), 80));
                    g.fillRect(x0 + rng.nextInt(4), y0 + rng.nextInt(4),
                            (w / subGrid) / 2 + rng.nextInt(w / subGrid / 2),
                            (h / subGrid) / 2 + rng.nextInt(h / subGrid / 2));
                }
            }
        }
    }

    private static void generateMartianTexture(Graphics2D g, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2, h = TEXTURE_SIZE;
        g.setColor(base);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 40 + rng.nextInt(40); i++) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            int sz = 5 + rng.nextInt(30);
            Color darker = new Color(
                    Math.max(0, base.getRed() - 40 - rng.nextInt(40)),
                    Math.max(0, base.getGreen() - 30 - rng.nextInt(30)),
                    Math.max(0, base.getBlue() - 20 - rng.nextInt(20))
            );
            g.setColor(darker);
            g.fillOval(x, y, sz, sz);
        }
        for (int i = 0; i < 10 + rng.nextInt(10); i++) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            int sz = 40 + rng.nextInt(80);
            Color lighter = new Color(
                    Math.min(255, base.getRed() + 20 + rng.nextInt(20)),
                    Math.min(255, base.getGreen() + 15 + rng.nextInt(15)),
                    Math.min(255, base.getBlue() + 10 + rng.nextInt(10))
            );
            g.setColor(lighter);
            g.fillOval(x - sz / 2, y - sz / 2, sz, sz);
        }
    }

    private static void generateGasGiantTexture(Graphics2D g, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2, h = TEXTURE_SIZE;
        int bands = 6 + rng.nextInt(12);
        for (int b = 0; b < bands; b++) {
            int y0 = b * h / bands;
            int y1 = (b + 1) * h / bands;
            int bandH = y1 - y0;
            Color bandColor = varyColor(base, rng, 40);
            g.setColor(bandColor);
            g.fillRect(0, y0, w, bandH);
            if (rng.nextBoolean()) {
                g.setColor(varyColor(bandColor, rng, 20));
                int swirlY = y0 + rng.nextInt(bandH);
                int swirlH = 1 + rng.nextInt(4);
                g.fillRect(0, swirlY, w, swirlH);
            }
        }
        for (int i = 0; i < 5 + rng.nextInt(10); i++) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            int sz = 10 + rng.nextInt(30);
            g.setColor(new Color(255, 255, 255, 30 + rng.nextInt(40)));
            g.fillOval(x, y, sz, sz);
        }
    }

    private static void generateRockyTexture(Graphics2D g, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2, h = TEXTURE_SIZE;
        g.setColor(base);
        g.fillRect(0, 0, w, h);
        int craters = 20 + rng.nextInt(30);
        for (int i = 0; i < craters; i++) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            int sz = 3 + rng.nextInt(25);
            g.setColor(new Color(
                    Math.max(0, base.getRed() - 30 - rng.nextInt(30)),
                    Math.max(0, base.getGreen() - 30 - rng.nextInt(30)),
                    Math.max(0, base.getBlue() - 30 - rng.nextInt(30))
            ));
            g.fillOval(x, y, sz, sz);
            g.setColor(new Color(
                    Math.min(255, base.getRed() + 20 + rng.nextInt(20)),
                    Math.min(255, base.getGreen() + 20 + rng.nextInt(20)),
                    Math.min(255, base.getBlue() + 20 + rng.nextInt(20))
            ));
            g.fillOval(x + 1, y + 1, Math.max(1, sz - 4), Math.max(1, sz - 4));
        }
    }

    private static void generateGenericTexture(Graphics2D g, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2, h = TEXTURE_SIZE;
        g.setColor(base);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 100 + rng.nextInt(100); i++) {
            int x = rng.nextInt(w);
            int y = rng.nextInt(h);
            int sz = 2 + rng.nextInt(10);
            g.setColor(varyColor(base, rng, 60));
            g.fillRect(x, y, sz, sz);
        }
    }

    private static void addAtmosphereEffect(BufferedImage img, Graphics2D g, Color base, Random rng) {
        int w = TEXTURE_SIZE * 2;
        int h = TEXTURE_SIZE;
        int cx = w / 2;
        int cy = h / 2;
        float maxDist = (float) Math.sqrt(cx * cx + cy * cy);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy) / maxDist;
                if (dist > 0.7f) {
                    float alpha = (dist - 0.7f) / 0.3f * 0.3f;
                    int px = img.getRGB(x, y);
                    int r = Math.min(255, (int) ((px >> 16 & 0xFF) * (1 - alpha) + 180 * alpha));
                    int gr = Math.min(255, (int) ((px >> 8 & 0xFF) * (1 - alpha) + 180 * alpha));
                    int b = Math.min(255, (int) ((px & 0xFF) * (1 - alpha) + 255 * alpha));
                    img.setRGB(x, y, (255 << 24) | (r << 16) | (gr << 8) | b);
                }
            }
        }
    }

    private static Color varyColor(Color base, Random rng, int amount) {
        return new Color(
                clamp(base.getRed() + rng.nextInt(amount * 2) - amount),
                clamp(base.getGreen() + rng.nextInt(amount * 2) - amount),
                clamp(base.getBlue() + rng.nextInt(amount * 2) - amount)
        );
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static BufferedImage generateSunTexture(int color, double temperature) {
        BufferedImage img = new BufferedImage(TEXTURE_SIZE * 2, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Random rng = new Random((long) (temperature * 100));

        Color sunColor = new Color(color, true);
        g.setColor(sunColor);
        g.fillRect(0, 0, TEXTURE_SIZE * 2, TEXTURE_SIZE);

        int cells = 8 + rng.nextInt(8);
        for (int gy = 0; gy < cells; gy++) {
            for (int gx = 0; gx < cells; gx++) {
                Color cell = varyColor(sunColor, rng, 20);
                g.setColor(cell);
                int x0 = gx * TEXTURE_SIZE * 2 / cells;
                int y0 = gy * TEXTURE_SIZE / cells;
                int x1 = (gx + 1) * TEXTURE_SIZE * 2 / cells;
                int y1 = (gy + 1) * TEXTURE_SIZE / cells;
                g.fillRect(x0, y0, x1 - x0, y1 - y0);
            }
        }

        for (int i = 0; i < 20 + rng.nextInt(30); i++) {
            int x = rng.nextInt(TEXTURE_SIZE * 2);
            int y = rng.nextInt(TEXTURE_SIZE);
            int sz = 4 + rng.nextInt(20);
            g.setColor(new Color(255, 255, 255, 40 + rng.nextInt(60)));
            g.fillOval(x, y, sz, sz);
        }

        g.dispose();
        return img;
    }
}
