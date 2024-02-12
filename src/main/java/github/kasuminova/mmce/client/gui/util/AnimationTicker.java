package github.kasuminova.mmce.client.gui.util;


public class AnimationTicker {
    private final int animationTime;
    private final long startTime;
    private final float[] bezierControlPoints;

    public AnimationTicker(int animationTime, float... bezierControlPoints) {
        if (bezierControlPoints.length < 2 || bezierControlPoints.length % 2 != 0) {
            throw new IllegalArgumentException("Bezier control points must be provided in pairs, and there must be at least two pairs.");
        }
        if (animationTime <= 0) {
            throw new IllegalArgumentException("AnimationTime must be larger than 0.");
        }

        this.animationTime = animationTime;
        this.startTime = System.currentTimeMillis();
        this.bezierControlPoints = bezierControlPoints;
    }

    public float getAnimationPercent() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        float percent = (float) elapsedTime / animationTime;

        return Math.max(Math.min(cubicBezier(percent, bezierControlPoints), 1.0F), 0F);
    }

    private float cubicBezier(float t, float... controlPoints) {
        int numSegments = (controlPoints.length - 2) / 2;

        float x = 0, y = 0;
        for (int i = 0; i < numSegments; i++) {
            float p0x = controlPoints[i * 2];
            float p0y = controlPoints[i * 2 + 1];
            float p1x = controlPoints[i * 2 + 2];
            float p1y = controlPoints[i * 2 + 3];

            x = (1 - t) * p0x + t * p1x;
            y = (1 - t) * p0y + t * p1y;

            t = (1 - t) * p0x + t * p1x;
        }

        return y;
    }
}
