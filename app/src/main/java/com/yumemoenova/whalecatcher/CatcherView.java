package com.yumemoenova.whalecatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Random;

final class CatcherView extends View {
    private static final float W = GameState.WIDTH;
    private static final int WHITE = 0xfff3fbff;
    private static final int ICE = 0xffa6ecff;
    private static final int GOLD = 0xffffd78a;
    private static final int LAVENDER = 0xffa9b6ea;
    private static final int INK = 0xff0c1334;
    private final Paint paint = new Paint();
    private final Paint spritePaint = new Paint();
    private final RectF destination = new RectF();
    private final Path polygon = new Path();
    private final SharedPreferences history;
    private final GameState game;
    private final Bitmap hero, host, coupon, walkLeft, walkRight, heroJump;
    private final float[] stars = new float[144];
    private final Typeface brand = Typeface.create("sans-serif-black", Typeface.BOLD);
    private final Typeface numbers = Typeface.create("monospace", Typeface.BOLD);
    private float unit = 1, offsetX, offsetY, h = 680, ambience, joystick;
    private int stickId = -1, jumpId = -1, savedBest;
    private boolean keyLeft, keyRight, active;
    private long lastFrame;
    private Shader sky;

    CatcherView(Context context) {
        super(context);
        setFocusableInTouchMode(true);
        requestFocus();
        history = context.getSharedPreferences("whale_ticket_scores", Context.MODE_PRIVATE);
        savedBest = history.getInt("top_score", 0);
        game = new GameState(new Random(), savedBest);
        hero = load("player.png");
        walkLeft = load("walk-left.png");
        walkRight = load("walk-right.png");
        heroJump = load("jump.png");
        host = load("host.png");
        coupon = load("ticket.png");
        spritePaint.setFilterBitmap(false);
        spritePaint.setDither(false);
        Random random = new Random(407);
        for (int i = 0; i < stars.length; i += 3) {
            stars[i] = random.nextFloat() * W;
            stars[i + 1] = random.nextFloat();
            stars[i + 2] = 1 + random.nextInt(3);
        }
    }

    private Bitmap load(String filename) {
        try (InputStream stream = getContext().getAssets().open(filename)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inSampleSize = 4;
            Bitmap source = BitmapFactory.decodeStream(stream, null, opts);
            if (source == null) throw new IllegalStateException("Cannot decode " + filename);
            int w = source.getWidth(), h = source.getHeight();
            int left = w, top = h, right = 0, bottom = 0;
            int[] row = new int[w];
            for (int y = 0; y < h; y++) {
                source.getPixels(row, 0, w, 0, y, w, 1);
                for (int x = 0; x < w; x++) {
                    if ((row[x] >>> 24) < 128) continue;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
            return left < right && top < bottom
                    ? Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
                    : source;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot open " + filename, error);
        }
    }

    void setActive(boolean active) {
        this.active = active;
        lastFrame = 0;
        if (active) {
            invalidate();
        } else {
            clearControls();
            saveScore();
        }
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        unit = Math.min(width / W, height / 620f);
        if (unit <= 0) unit = 1;
        h = Math.min(950f, height / unit);
        offsetX = (width - W * unit) / 2;
        offsetY = (height - h * unit) / 2;
        game.floor = h - 150;
        sky = new LinearGradient(0, 0, 0, h,
                new int[] {0xff131a44, 0xff23316a, 0xff125577, 0xff101b44},
                new float[] {0f, 0.38f, 0.78f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.034f, (now - lastFrame) * 1e-9f);
        lastFrame = now;
        if (active) {
            ambience += dt;
            game.tick(dt, getAxis());
            if (game.over) saveScore();
        }
        canvas.drawColor(INK);
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(unit, unit);
        canvas.clipRect(0, 0, W, h);
        drawStage(canvas);
        drawHost(canvas);
        for (GameState.Ticket ticket : game.tickets) drawTicket(canvas, ticket);
        drawPlayer(canvas);
        drawSparks(canvas);
        drawHud(canvas);
        drawControls(canvas);
        if (!game.started || game.over) drawMenu(canvas);
        canvas.restore();
        if (active) postInvalidateOnAnimation();
    }

    private float getAxis() {
        float key = (keyRight ? 1 : 0) - (keyLeft ? 1 : 0);
        return key == 0 ? joystick : key;
    }

    private void drawStage(Canvas canvas) {
        paint.setShader(sky);
        canvas.drawRect(0, 0, W, h, paint);
        paint.setShader(null);
        float water = game.floor - 104;
        for (int i = 0; i < stars.length; i += 3) {
            float x = stars[i], y = 74 + stars[i + 1] * (water - 73), size = stars[i + 2];
            int color = (i % 15 == 0) ? 0xffb2dafd : 0xff536ca4;
            if (i % 12 == 0 && Math.sin(ambience * 2.8 + i) > 0.2) size += 2;
            rect(canvas, x, y, x + size, y + size, color);
        }
        circle(canvas, 190, 272, 99, 0x1437e3fb);
        circle(canvas, 190, 272, 76, 0x1237e3fb);
        circle(canvas, 307, 287, 36, 0x2244a8ed);
        circle(canvas, 307, 287, 31, 0xff546baf);
        circle(canvas, 307, 287, 27, 0xffbdc9fe);
        circle(canvas, 318, 275, 25, 0xff24447a);

        for (int band = 0; band < 4; band++) {
            float y = water + band * 24;
            rect(canvas, 0, y, W, y + 23, band % 2 == 0 ? 0xff174c73 : 0xff174368);
            for (int k = -1; k < 8; k++) {
                float x = k * 61 + (ambience * (5 + 2 * band)) % 61;
                rect(canvas, x, y + 1, x + 37, y + 3, 0xff3982a2);
                rect(canvas, x + 28, y + 7, x + 49, y + 8, 0xff5fb1c6);
            }
        }
        rect(canvas, 0, game.floor, W, game.floor + 9, 0xff6ed0dc);
        rect(canvas, 0, game.floor + 8, W, game.floor + 20, 0xff2d7f94);
        rect(canvas, 0, game.floor + 20, W, game.floor + 25, 0xff173d68);
        for (int i = 1; i < W; i += 32) {
            rect(canvas, i, game.floor + 11, i + 21, game.floor + 13, 0xff4fa1b7);
            rect(canvas, i + 10, game.floor + 23, i + 26, game.floor + 26, 0xff0b294b);
        }
        rect(canvas, 0, game.floor + 27, W, h, 0xff101839);
        rect(canvas, 0, game.floor + 31, W, game.floor + 32, 0xff435185);
        for (int x = 18; x < W; x += 13) {
            for (int y = (int) game.floor + 40; y < h - 8; y += 13)
                rect(canvas, x, y, x + 1, y + 1, 0xff273459);
        }
        for (int i = 0; i < 2; i++) {
            float x = i == 0 ? 6 : W - 13;
            rect(canvas, x, 90, x + 7, game.floor, 0x502a4581);
            for (float y = 98; y < game.floor; y += 21)
                rect(canvas, x + 1, y, x + 3, y + 8, 0x608fcbe6);
        }
    }

    private void drawHost(Canvas canvas) {
        float x = game.started ? game.hostX : W / 2 + 58 * (float) Math.sin(ambience * 0.72f);
        float top = 76 + 4 * (float) Math.sin(ambience * 4.8f);
        drawFit(canvas, host, x, top, 142, 137);
        float y = 201;
        rect(canvas, x - 74, y + 5, x + 74, y + 19, 0xff142952);
        rect(canvas, x - 74, y + 3, x + 74, y + 7, 0xffb4e9f5);
        rect(canvas, x - 69, y + 8, x + 69, y + 11, 0xff658fd1);
        rect(canvas, x - 54, y + 18, x - 14, y + 21, 0xff66c3dc);
        rect(canvas, x + 14, y + 18, x + 54, y + 21, 0xff66c3dc);
        if (game.started && game.nextDrop < 0.2f)
            sparkle(canvas, x + (game.hostVelocity < 0 ? -12 : 12), 199, 8, GOLD);
    }

    private void drawTicket(Canvas canvas, GameState.Ticket ticket) {
        canvas.save();
        canvas.translate(Math.round(ticket.x), Math.round(ticket.y));
        canvas.rotate(ticket.angle);
        drawFit(canvas, coupon, 0, -12, 43, 24);
        canvas.restore();
    }

    private void drawPlayer(Canvas canvas) {
        float x = Math.round(game.playerX);
        float top = Math.round(game.floor - game.lift - GameState.PLAYER_HEIGHT);
        boolean moving = Math.abs(game.speed) > 8;
        boolean jumping = game.lift > 0;
        Bitmap frame = jumping ? heroJump : moving ? (game.facing < 0 ? walkLeft : walkRight) : hero;
        float w = jumping ? 98 : 104;
        float height = GameState.PLAYER_HEIGHT;
        float shadowSize = Math.max(20, 41 - game.lift * 0.2f);
        oval(canvas, x - shadowSize, game.floor - 8, x + shadowSize, game.floor + 3, 0x7530415d);
        canvas.save();
        canvas.translate(x, top);
        float bounce = moving && !jumping ? 1.5f * (float) Math.sin(game.gait) : 0;
        drawFit(canvas, frame, 0, bounce, w, height);
        canvas.restore();
        if (game.catchFlash > 0) {
            sparkle(canvas, x - 40, top + 13, 7, GOLD);
            sparkle(canvas, x + 41, top + 7, 8, GOLD);
        }
    }

    private void drawSparks(Canvas canvas) {
        for (GameState.Spark spark : game.sparks) {
            int alpha = (int) (255 * spark.life / 0.46f);
            int tint = spark.caught ? 0x00ffe6a3 : 0x00f48dad;
            rect(canvas, spark.x, spark.y, spark.x + 3, spark.y + 3, tint | (alpha << 24));
        }
        if (game.catchFlash > 0 && game.combo > 1) {
            float lift = (0.4f - game.catchFlash) * 32;
            label(canvas, "连击 ×" + game.combo, game.playerX, game.bowlY() - 21 - lift, 15, GOLD, Paint.Align.CENTER, true);
        }
    }

    private void drawHud(Canvas canvas) {
        panel(canvas, 16, 14, 208, 74, 0xee141c40, 0xff506aa6);
        label(canvas, "鲸元券接接乐", 28, 39, 21, WHITE, Paint.Align.LEFT, true);
        label(canvas, "WHALE TICKET ARCADE", 29, 57, 9, ICE, Paint.Align.LEFT, true);

        panel(canvas, 219, 14, 368, 74, 0xee141c40, 0xff739cce);
        label(canvas, "得分", 229, 32, 11, ICE, Paint.Align.LEFT, true);
        label(canvas, "最高 " + game.best, 357, 33, 11, LAVENDER, Paint.Align.RIGHT, false);
        number(canvas, String.format(Locale.ROOT, "%04d", game.score), 229, 56, 22, GOLD);
        for (int i = 0; i < GameState.START_HEARTS; i++)
            heart(canvas, 300 + i * 12, 47, i < game.hearts ? 0xffff7d9c : 0xff41496a);
    }

    private void drawControls(Canvas canvas) {
        float y = h - 62, jx = 91;
        circle(canvas, jx, y, 44, 0xff273762);
        circle(canvas, jx, y, 42, 0xff141e45);
        circle(canvas, jx, y, 31, 0xff1e3156);
        float stick = 21 * joystick;
        circle(canvas, jx + stick, y + 2, 22, 0xff568ab6);
        circle(canvas, jx + stick, y, 19, 0xff83c7df);
        circle(canvas, jx + stick, y - 3, 12, 0xffa5e5ed);
        poly(canvas, 0xff77a7cd, 44,y, 54,y-6, 54,y+6);
        poly(canvas, 0xff77a7cd, 138,y, 128,y-6, 128,y+6);

        float jumpX = W - 82;
        circle(canvas, jumpX, y + 4, 43, 0xff302d5a);
        circle(canvas, jumpX, y, 41, jumpId == -1 ? 0xffad96f5 : 0xfff1ccff);
        circle(canvas, jumpX, y - 1, 34, 0xff634fbc);
        poly(canvas, WHITE, jumpX,y-23, jumpX-10,y-12, jumpX-4,y-12,
                jumpX-4,y-2,jumpX+4,y-2,jumpX+4,y-12,jumpX+10,y-12);
        label(canvas, "跳跃", jumpX, y + 18, 15, WHITE, Paint.Align.CENTER, true);
        label(canvas, "移动", jx, h - 13, 10, LAVENDER, Paint.Align.CENTER, true);
    }

    private float menuCenter() { return 235 + (h - 620) * 0.45f; }

    private void drawMenu(Canvas canvas) {
        float y = menuCenter();
        rect(canvas, 0, 226, W, game.floor, 0x24223060);
        panel(canvas, 29, y, W - 29, y + 171, 0xf0192149, 0xff9dc3e9);
        sparkle(canvas, 58, y + 34, 9, GOLD);
        sparkle(canvas, W - 58, y + 34, 9, GOLD);
        label(canvas, game.over ? "挑战结束" : "鲸元券接接乐", W / 2, y + 43, 31, WHITE, Paint.Align.CENTER, true);
        label(canvas, game.over ? "得分 " + game.score + "   ·   最高 " + game.best : "头顶钢盆，接住飘落的鲸元券",
                W / 2, y + 70, 14, ICE, Paint.Align.CENTER, true);
        label(canvas, game.over ? "连续接券可以获得连击加分" : "左侧摇杆移动 · 右侧按键跳跃",
                W / 2, y + 92, 13, LAVENDER, Paint.Align.CENTER, false);
        panel(canvas, 104, y + 108, W - 104, y + 150, 0xff416fb2, 0xff9ce8f3);
        label(canvas, game.over ? "再玩一次" : "开始游戏", W / 2, y + 136, 19, WHITE, Paint.Align.CENTER, true);
    }

    private void saveScore() {
        if (game.best > savedBest) {
            savedBest = game.best;
            history.edit().putInt("top_score", savedBest).apply();
        }
    }

    private void reset() {
        saveScore();
        clearControls();
        game.start();
    }

    private void clearControls() {
        stickId = jumpId = -1;
        joystick = 0;
        keyLeft = keyRight = false;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int phase = event.getActionMasked();
        int index = event.getActionIndex();
        if (phase == MotionEvent.ACTION_CANCEL) {
            clearControls();
            return true;
        }
        if (phase == MotionEvent.ACTION_DOWN || phase == MotionEvent.ACTION_POINTER_DOWN) {
            float x = (event.getX(index) - offsetX) / unit;
            float y = (event.getY(index) - offsetY) / unit;
            int id = event.getPointerId(index);
            if (!game.started || game.over) {
                float row = menuCenter();
                if (x > 96 && x < W - 96 && y > row + 100 && y < row + 160) {
                    reset();
                    performClick();
                }
                return true;
            }
            if (y >= game.floor + 27) {
                if (x <= W / 2 && stickId == -1) {
                    stickId = id;
                    joystick = GameState.clamp((x - 91) / 40, -1, 1);
                } else if (x >= W * 0.55f && jumpId == -1) {
                    jumpId = id;
                    game.jump();
                }
            }
        } else if (phase == MotionEvent.ACTION_MOVE && stickId != -1) {
            int pointer = event.findPointerIndex(stickId);
            if (pointer != -1) joystick = GameState.clamp(((event.getX(pointer) - offsetX) / unit - 91) / 40, -1, 1);
        } else if (phase == MotionEvent.ACTION_POINTER_UP || phase == MotionEvent.ACTION_UP) {
            int id = event.getPointerId(index);
            if (id == stickId) {
                stickId = -1;
                joystick = 0;
            }
            if (id == jumpId) jumpId = -1;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) keyLeft = true;
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) keyRight = true;
        else if (key == KeyEvent.KEYCODE_SPACE || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_ENTER) {
            if (event.getRepeatCount() == 0) {
                if (!game.started || game.over) reset(); else game.jump();
            }
        } else return super.onKeyDown(key, event);
        return true;
    }

    @Override public boolean onKeyUp(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) keyLeft = false;
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) keyRight = false;
        else return super.onKeyUp(key, event);
        return true;
    }

    private void drawFit(Canvas canvas, Bitmap asset, float x, float top, float maxWidth, float maxHeight) {
        float ratio = Math.min(maxWidth / asset.getWidth(), maxHeight / asset.getHeight());
        float w = asset.getWidth() * ratio, h = asset.getHeight() * ratio;
        destination.set(Math.round(x - w / 2), Math.round(top), Math.round(x + w / 2), Math.round(top + h));
        canvas.drawBitmap(asset, null, destination, spritePaint);
    }

    private void panel(Canvas c, float l, float t, float r, float b, int fill, int border) {
        poly(c, 0x80080d24, l+4,t+8,r-3,t+8,r+3,t+13,r+3,b,r-3,b+7,l+4,b+7,l-2,b,l-2,t+13);
        poly(c, border, l+5,t,r-5,t,r,t+5,r,b-5,r-5,b,l+5,b,l,b-5,l,t+5);
        poly(c, fill, l+5,t+2,r-5,t+2,r-2,t+5,r-2,b-5,r-5,b-2,l+5,b-2,l+2,b-5,l+2,t+5);
    }

    private void heart(Canvas c, float x, float y, int color) {
        poly(c,color,x,y+2,x+2,y,x+4,y,x+5,y+2,x+6,y,x+8,y,x+10,y+2,x+10,y+5,x+5,y+10,x,y+5);
    }

    private void sparkle(Canvas c, float x, float y, float size, int color) {
        rect(c,x-1,y-size,x+1,y+size,color);
        rect(c,x-size,y-1,x+size,y+1,color);
        rect(c,x-2,y-2,x+2,y+2,color);
    }

    private void label(Canvas c, String text, float x, float y, float size, int color, Paint.Align align, boolean bold) {
        paint.setAntiAlias(true);
        paint.setTypeface(bold ? brand : Typeface.DEFAULT);
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setColor(color);
        c.drawText(text,x,y,paint);
        paint.setAntiAlias(false);
    }

    private void number(Canvas c, String text, float x, float y, float size, int color) {
        paint.setTypeface(numbers);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(color);
        c.drawText(text,x,y,paint);
    }

    private void rect(Canvas c, float l, float t, float r, float b, int color) {
        paint.setColor(color);
        c.drawRect(l,t,r,b,paint);
    }

    private void circle(Canvas c, float x, float y, float radius, int color) {
        paint.setColor(color);
        c.drawCircle(x,y,radius,paint);
    }

    private void oval(Canvas c, float l, float t, float r, float b, int color) {
        paint.setColor(color);
        c.drawOval(l,t,r,b,paint);
    }

    private void poly(Canvas c, int color, float... p) {
        polygon.rewind();
        polygon.moveTo(p[0],p[1]);
        for (int i = 2; i < p.length; i += 2) polygon.lineTo(p[i],p[i+1]);
        polygon.close();
        paint.setColor(color);
        c.drawPath(polygon,paint);
    }
}
