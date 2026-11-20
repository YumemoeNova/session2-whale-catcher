package com.yumemoenova.whalecatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

final class GameState {
    static final float WIDTH = 384;
    static final int START_HEARTS = 5;
    static final float PLAYER_HEIGHT = 102;
    static final float BOWL_HALF_WIDTH = 37;

    static final class Ticket {
        float x, y, vx, vy, angle, spin;
    }

    static final class Spark {
        float x, y, vx, vy, life;
        final boolean caught;

        Spark(float x, float y, float vx, float vy, boolean caught) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.caught = caught;
            life = 0.46f;
        }
    }

    final ArrayList<Ticket> tickets = new ArrayList<>();
    final ArrayList<Spark> sparks = new ArrayList<>();
    private final Random random;
    float floor = 535, playerX = WIDTH / 2, lift, jumpVelocity, hostX = WIDTH / 2;
    float hostVelocity = 98, speed, gait, seconds, catchFlash, missFlash;
    float nextDrop, nextTurn;
    int score, combo, hearts = START_HEARTS, best, facing = 1;
    boolean started, over;

    GameState(Random random, int best) {
        this.random = random;
        this.best = best;
    }

    void start() {
        tickets.clear();
        sparks.clear();
        playerX = hostX = WIDTH / 2;
        lift = jumpVelocity = speed = gait = seconds = catchFlash = missFlash = 0;
        hostVelocity = 92;
        nextDrop = 0.48f;
        nextTurn = 1.1f;
        score = combo = 0;
        facing = 1;
        hearts = START_HEARTS;
        started = true;
        over = false;
    }

    void jump() {
        if (started && !over && lift == 0) jumpVelocity = 399;
    }

    float bowlY() { return floor - lift - PLAYER_HEIGHT + 9; }

    void tick(float dt, float axis) {
        if (!started || over) return;
        seconds += dt;
        catchFlash = Math.max(0, catchFlash - dt);
        missFlash = Math.max(0, missFlash - dt);
        float previousBowl = bowlY();
        speed = axis * 240;
        if (Math.abs(axis) > 0.06f) {
            facing = axis < 0 ? -1 : 1;
            gait += dt * 14;
        }
        playerX = clamp(playerX + speed * dt, 45, WIDTH - 45);
        if (jumpVelocity != 0 || lift > 0) {
            lift += jumpVelocity * dt;
            jumpVelocity -= 935 * dt;
            if (lift <= 0) lift = jumpVelocity = 0;
        }

        nextTurn -= dt;
        hostX += hostVelocity * dt;
        if (hostX < 65 || hostX > WIDTH - 65) {
            hostX = clamp(hostX, 65, WIDTH - 65);
            hostVelocity = -hostVelocity;
            nextTurn = 0.75f + random.nextFloat() * 1.6f;
        } else if (nextTurn <= 0) {
            if (random.nextBoolean()) hostVelocity = -hostVelocity;
            hostVelocity = Math.copySign(74 + random.nextFloat() * 74, hostVelocity);
            nextTurn = 0.55f + random.nextFloat() * 1.45f;
        }

        nextDrop -= dt;
        if (nextDrop <= 0) {
            Ticket ticket = new Ticket();
            ticket.x = hostX + (hostVelocity < 0 ? -12 : 12);
            ticket.y = 195;
            ticket.vx = (random.nextFloat() - 0.5f) * 36 + hostVelocity * 0.19f;
            ticket.vy = 66 + random.nextFloat() * 36;
            ticket.spin = (random.nextFloat() - 0.5f) * 88;
            tickets.add(ticket);
            float interval = Math.max(0.46f, 1.15f - seconds * 0.009f);
            nextDrop = interval + random.nextFloat() * 0.45f;
        }

        Iterator<Ticket> iterator = tickets.iterator();
        while (iterator.hasNext()) {
            Ticket ticket = iterator.next();
            float priorY = ticket.y;
            ticket.x += ticket.vx * dt;
            if (ticket.x < 20 || ticket.x > WIDTH - 20) {
                ticket.x = clamp(ticket.x, 20, WIDTH - 20);
                ticket.vx = -ticket.vx;
            }
            ticket.vy = Math.min(350, ticket.vy + 210 * dt);
            ticket.y += ticket.vy * dt;
            ticket.angle += ticket.spin * dt;
            if (priorY + 7 <= previousBowl + 2 && ticket.y + 7 >= bowlY()
                    && Math.abs(ticket.x - playerX) <= BOWL_HALF_WIDTH) {
                combo++;
                score += 10 + Math.min(40, (combo - 1) * 2);
                best = Math.max(best, score);
                catchFlash = 0.4f;
                burst(ticket.x, bowlY(), true);
                iterator.remove();
            } else if (ticket.y > floor + 10) {
                hearts = Math.max(0, hearts - 1);
                combo = 0;
                missFlash = 0.37f;
                burst(ticket.x, floor, false);
                iterator.remove();
                if (hearts == 0) {
                    over = true;
                    break;
                }
            }
        }

        Iterator<Spark> effects = sparks.iterator();
        while (effects.hasNext()) {
            Spark spark = effects.next();
            spark.x += spark.vx * dt;
            spark.y += spark.vy * dt;
            spark.vy += 180 * dt;
            spark.life -= dt;
            if (spark.life <= 0) effects.remove();
        }
    }

    private void burst(float x, float y, boolean caught) {
        for (int i = 0; i < 10; i++) {
            sparks.add(new Spark(x, y, (random.nextFloat() - 0.5f) * 186,
                    -28 - random.nextFloat() * 119, caught));
        }
    }

    static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
