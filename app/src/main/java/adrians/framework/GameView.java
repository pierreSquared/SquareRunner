package adrians.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import adrians.framework.util.InputHandler;
import adrians.framework.util.Painter;
import adrians.game.state.LoadState;
import adrians.game.state.State;
import adrians.game.state.StateManager;

/**
 * Created by pierre on 06/02/16.
 */
public class GameView extends SurfaceView implements Runnable{
    private Bitmap gameImage;
    private Rect gameImageSrc, gameImageDesc;
    private Canvas gameCanvas;
    private Painter graphics;

    private Thread gameThread;
    private volatile boolean running = false;
    private InputHandler inputHandler;

//    private boolean showFps = true;

    public GameView(Context context, int gameWidth, int gameHeight) {
        super(context);
        StateManager.setContext(context);
        gameImage = Bitmap.createBitmap(gameWidth, gameHeight, Bitmap.Config.RGB_565);
        gameImageSrc = new Rect(0, 0, gameImage.getWidth(), gameImage.getHeight());
        gameImageDesc = new Rect();
        gameCanvas = new Canvas(gameImage);
        graphics = new Painter(gameCanvas);

        SurfaceHolder holder = getHolder();
        holder.addCallback(new Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initInput();
                if (StateManager.getCurrentState() == null) {
                    StateManager.pushState(new LoadState());
                }
                initGame();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                pauseGame();
            }
        });
    }
    public  GameView(Context context) {
        super(context);
    }

    public State getCurrentState() {
        return StateManager.getCurrentState();
    }

    private void initInput() {
        if(inputHandler == null) {
            inputHandler = new InputHandler();
        }
        setOnTouchListener(inputHandler);
    }

    @Override
    public void run() {
        long updateDurationNanos = 0, sleepDurationNanos = 0;
        while (running) {
            long beforeUpdateAndRender = System.nanoTime();
            long deltaNanos = sleepDurationNanos + updateDurationNanos;
            updateAndRender(deltaNanos, beforeUpdateAndRender);
            updateDurationNanos = (System.nanoTime() - beforeUpdateAndRender);
            sleepDurationNanos = Math.max(1, (int)(16+2f/3-updateDurationNanos/1e6f));

            try {
                Thread.sleep((int)(sleepDurationNanos/1e6));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initGame() {
        running = true;
        gameThread = new Thread(this, "Game Thread");
        gameThread.start();
    }

    private void pauseGame() {
        running = false;
        while(gameThread.isAlive()) {
            try {
                gameThread.join();
                break;
            } catch (InterruptedException e) {

            }
        }
    }

    private void updateAndRender(long delta, long nanoTime) {
        delta = Math.min(delta, (long ) (0.15f*1e9) );
        StateManager.getCurrentState().update(delta / 1e9f);
        StateManager.getCurrentState().render(graphics);
//        if(showFps) {
//            FpsCounter.update(nanoTime);
//            FpsCounter.printFps(graphics);
//        }
        renderGameImage();
    }

    private void renderGameImage() {
        Canvas screen = getHolder().lockCanvas();
        if(screen != null) {
            screen.getClipBounds(gameImageDesc);
            screen.drawBitmap(gameImage, gameImageSrc, gameImageDesc, null);
            getHolder().unlockCanvasAndPost(screen);

        }
    }

    public void onResume() {
        if(StateManager.getCurrentState()!=null) {
            StateManager.getCurrentState().onResume();
        }
    }
    public void onPause() {
        if(StateManager.getCurrentState()!=null) {
            StateManager.getCurrentState().onPause();
        }
    }
}
