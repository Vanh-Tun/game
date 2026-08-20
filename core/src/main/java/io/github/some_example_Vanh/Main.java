package io.github.some_example_Vanh;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Iterator;

public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;

    // =========================
    // HÌNH ẢNH
    // =========================

    private Texture background;
    private Texture tom;
    private Texture zombieThuong;
    private Texture zombieDoiXo;
    private Texture zombieBay;
    private Texture vienDan;
    private Texture heartIcon;

    // =========================
    // TOM
    // =========================

    private float tomX;
    private float tomY;

    private final float TOM_WIDTH = 110;
    private final float TOM_HEIGHT = 130;

    // =========================
    // GAME
    // =========================

    private Array<Zombie> zombies;
    private Array<Bullet> bullets;

    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;

    private float spawnTimer = 0;
    private float spawnDelay = 1.2f;

    private int zombieType = 0;

    private float shootTimer = 0;
    private final float SHOOT_DELAY = 0.3f;


    @Override
    public void create() {

        batch = new SpriteBatch();

        font = new BitmapFont();
        font.getData().setScale(2);
        font.setColor(Color.WHITE);

        background = new Texture("background.png");

        tom = new Texture("tom.png");

        // Zombie và đạn dùng hàm lọc nền trắng thuần (không làm lủng xô)
        zombieThuong = loadImageWithoutWhite("zombiethuong.png");
        zombieDoiXo = loadImageWithoutWhite("zombiedoixo.png");
        zombieBay = loadImageWithoutWhite("zombiebay.png");
        vienDan = loadImageWithoutWhite("viendan.png");

        // Trái tim dùng hàm lọc riêng biệt (xóa sạch ô xám caro)
        heartIcon = loadHeartWithoutBackground("traitim.png");

        tomX = Gdx.graphics.getWidth() / 2f - TOM_WIDTH / 2f;
        tomY = 30;

        zombies = new Array<>();
        bullets = new Array<>();
    }


    // =========================================================
    // HÀM LỌC NỀN DÀNH RIÊNG CHO ZOMBIE (CHỈ XÓA TRẮNG THUẦN)
    // =========================================================

    private Texture loadImageWithoutWhite(String fileName) {

        Pixmap original = new Pixmap(
            Gdx.files.internal(fileName)
        );

        Pixmap result = new Pixmap(
            original.getWidth(),
            original.getHeight(),
            Pixmap.Format.RGBA8888
        );

        for (int x = 0; x < original.getWidth(); x++) {

            for (int y = 0; y < original.getHeight(); y++) {

                int pixel = original.getPixel(x, y);

                int r = (pixel >>> 24) & 255;
                int g = (pixel >>> 16) & 255;
                int b = (pixel >>> 8) & 255;

                if (r > 245 && g > 245 && b > 245) {
                    result.drawPixel(x, y, 0);
                } else {
                    result.drawPixel(x, y, pixel);
                }
            }
        }

        Texture texture = new Texture(result);

        original.dispose();
        result.dispose();

        return texture;
    }


    // =========================================================
    // HÀM LỌC NỀN DÀNH RIÊNG CHO TRÁI TIM (XÓA CẢ Ô CARO XÁM/TRẮNG)
    // =========================================================

    private Texture loadHeartWithoutBackground(String fileName) {

        Pixmap original = new Pixmap(
            Gdx.files.internal(fileName)
        );

        Pixmap result = new Pixmap(
            original.getWidth(),
            original.getHeight(),
            Pixmap.Format.RGBA8888
        );

        for (int x = 0; x < original.getWidth(); x++) {

            for (int y = 0; y < original.getHeight(); y++) {

                int pixel = original.getPixel(x, y);

                int r = (pixel >>> 24) & 255;
                int g = (pixel >>> 16) & 255;
                int b = (pixel >>> 8) & 255;

                boolean isWhite = (r > 230 && g > 230 && b > 230);
                boolean isGrayChecker = (Math.abs(r - g) < 12 && Math.abs(g - b) < 12 && r > 170 && r < 230);

                // Xóa nếu là màu trắng hoặc màu xám caro giả trong suốt
                if (isWhite || isGrayChecker) {
                    result.drawPixel(x, y, 0);
                } else {
                    result.drawPixel(x, y, pixel);
                }
            }
        }

        Texture texture = new Texture(result);

        original.dispose();
        result.dispose();

        return texture;
    }


    @Override
    public void render() {

        float delta = Gdx.graphics.getDeltaTime();

        ScreenUtils.clear(0, 0, 0, 1);

        if (!gameOver) {

            handleInput(delta);

            spawnTimer += delta;

            if (spawnTimer >= spawnDelay) {
                spawnZombie();
                spawnTimer = 0;
            }

            updateZombies(delta);
            updateBullets(delta);
            checkCollision();

        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isTouched()) {
                restartGame();
            }
        }

        batch.begin();

        batch.draw(
            background,
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        batch.draw(
            tom,
            tomX,
            tomY,
            TOM_WIDTH,
            TOM_HEIGHT
        );

        for (Zombie zombie : zombies) {

            batch.draw(
                zombie.texture,
                zombie.x,
                zombie.y,
                zombie.width,
                zombie.height
            );
        }

        for (Bullet bullet : bullets) {

            batch.draw(
                vienDan,
                bullet.x,
                bullet.y,
                bullet.width,
                bullet.height
            );
        }

        font.draw(
            batch,
            "DIEM: " + score,
            20,
            Gdx.graphics.getHeight() - 20
        );

        // Vẽ biểu tượng trái tim
        float heartSize = 35f;
        float heartSpacing = 10f;
        float startX = 20f;
        float startY = Gdx.graphics.getHeight() - 90;

        for (int i = 0; i < lives; i++) {
            batch.draw(
                heartIcon,
                startX + i * (heartSize + heartSpacing),
                startY,
                heartSize,
                heartSize
            );
        }

        if (gameOver) {

            font.getData().setScale(3);

            font.draw(
                batch,
                "GAME OVER",
                Gdx.graphics.getWidth() / 2f - 140,
                Gdx.graphics.getHeight() / 2f + 40
            );

            font.getData().setScale(1.5f);

            font.draw(
                batch,
                "Nhan SPACE hoac cham man hinh de choi lai",
                Gdx.graphics.getWidth() / 2f - 210,
                Gdx.graphics.getHeight() / 2f - 20
            );

            font.getData().setScale(2);
        }

        batch.end();
    }


    private void restartGame() {
        score = 0;
        lives = 3;
        gameOver = false;
        zombies.clear();
        bullets.clear();
        spawnTimer = 0;
        tomX = Gdx.graphics.getWidth() / 2f - TOM_WIDTH / 2f;
    }


    private void handleInput(float delta) {

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            tomX -= 350 * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            tomX += 350 * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            shoot();
        }

        if (Gdx.input.isTouched()) {
            float touchX = Gdx.input.getX();

            if (touchX < Gdx.graphics.getWidth() / 2f) {
                tomX -= 300 * delta;
            } else {
                tomX += 300 * delta;
            }

            shoot();
        }

        if (tomX < 0) {
            tomX = 0;
        }

        if (tomX > Gdx.graphics.getWidth() - TOM_WIDTH) {
            tomX = Gdx.graphics.getWidth() - TOM_WIDTH;
        }

        shootTimer -= delta;
    }


    private void shoot() {

        if (shootTimer > 0) {
            return;
        }

        Bullet bullet = new Bullet();

        bullet.x = tomX + TOM_WIDTH / 2f - 12;
        bullet.y = tomY + TOM_HEIGHT - 10;
        bullet.width = 24;
        bullet.height = 45;
        bullet.speed = 650;

        bullets.add(bullet);

        shootTimer = SHOOT_DELAY;
    }


    private void spawnZombie() {

        Zombie zombie = new Zombie();

        zombie.width = 100;
        zombie.height = 110;

        zombie.x = (float) Math.random() * (Gdx.graphics.getWidth() - zombie.width);
        zombie.y = Gdx.graphics.getHeight();
        zombie.speed = 100 + (float) Math.random() * 80;

        if (zombieType == 0) {
            zombie.texture = zombieThuong;
            zombieType = 1;
        } else if (zombieType == 1) {
            zombie.texture = zombieDoiXo;
            zombieType = 2;
        } else {
            zombie.texture = zombieBay;
            zombieType = 0;
        }

        zombies.add(zombie);
    }


    private void updateZombies(float delta) {

        Rectangle tomRect = new Rectangle(tomX, tomY, TOM_WIDTH, TOM_HEIGHT);

        Iterator<Zombie> iterator = zombies.iterator();

        while (iterator.hasNext()) {

            Zombie zombie = iterator.next();
            zombie.y -= zombie.speed * delta;

            Rectangle zombieRect = new Rectangle(zombie.x, zombie.y, zombie.width, zombie.height);

            if (zombieRect.overlaps(tomRect)) {
                lives--;
                iterator.remove();

                if (lives <= 0) {
                    lives = 0;
                    gameOver = true;
                }

                continue;
            }

            if (zombie.y < -zombie.height) {
                iterator.remove();
            }
        }
    }


    private void updateBullets(float delta) {

        for (Bullet bullet : bullets) {
            bullet.y += bullet.speed * delta;
        }

        Iterator<Bullet> iterator = bullets.iterator();

        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            if (bullet.y > Gdx.graphics.getHeight()) {
                iterator.remove();
            }
        }
    }


    private void checkCollision() {

        Iterator<Bullet> bulletIterator = bullets.iterator();

        while (bulletIterator.hasNext()) {

            Bullet bullet = bulletIterator.next();
            Rectangle bulletRect = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);

            boolean hit = false;

            Iterator<Zombie> zombieIterator = zombies.iterator();

            while (zombieIterator.hasNext()) {

                Zombie zombie = zombieIterator.next();
                Rectangle zombieRect = new Rectangle(zombie.x, zombie.y, zombie.width, zombie.height);

                if (bulletRect.overlaps(zombieRect)) {
                    zombieIterator.remove();
                    score++;
                    hit = true;
                    break;
                }
            }

            if (hit) {
                bulletIterator.remove();
            }
        }
    }


    private static class Zombie {
        Texture texture;
        float x, y;
        float width, height;
        float speed;
    }


    private static class Bullet {
        float x, y;
        float width, height;
        float speed;
    }


    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        background.dispose();
        tom.dispose();
        zombieThuong.dispose();
        zombieDoiXo.dispose();
        zombieBay.dispose();
        vienDan.dispose();
        heartIcon.dispose();
    }
}
