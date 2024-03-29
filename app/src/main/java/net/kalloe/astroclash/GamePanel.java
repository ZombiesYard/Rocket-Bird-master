package net.kalloe.astroclash;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private long missileStartTime;
    private GameThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Rocket> rockets;
    private Random rand = new Random();
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean newGameCreated;
    //增加降低难度，减小增加难度
    private int progressDenom = 500;
    private int best;
    private SharedPrefManager prefManager;


    public GamePanel(Context context)
    {
        super(context);

        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);


        //make gamePanel focusable so it can handle events
        setFocusable(true);

        this.prefManager = new SharedPrefManager(getContext());
        this.best = prefManager.get(SharedPrefManager.PREF_BEST_SCORE);


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        int counter = 0;
        while(retry && counter<1000)
        {
            counter++;
            try{thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;

            }catch(InterruptedException e){e.printStackTrace();}

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.material_landscape));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.material_bird), 99, 66, 3);
        rockets = new ArrayList<Rocket>();
        missileStartTime = System.nanoTime();

        thread = new GameThread(getHolder(), this);



        thread.setRunning(true);
        thread.start();

    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(!player.getPlaying())
            {
                player.setPlaying(true);
                player.setUp(true);
            }
            else
            {
                player.setUp(true);
            }
            return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update()

    {
        if(player.getPlaying()) {

            bg.update();
            player.update();



            long missileElapsed = (System.nanoTime()-missileStartTime)/1000000;
            if(missileElapsed >(2000 - player.getScore()/4)){


                if(rockets.size()==0)
                {
                    rockets.add(new Rocket(BitmapFactory.decodeResource(getResources(),R.drawable.
                            missile),WIDTH + 10, HEIGHT/2, 45, 15, player.getScore(), 13));
                }
                else
                {

                    rockets.add(new Rocket(BitmapFactory.decodeResource(getResources(),R.drawable.missile),
                            WIDTH+10, (int)(rand.nextDouble()*(HEIGHT - (maxBorderHeight * 2))+maxBorderHeight),45,15, player.getScore(),13));
                }


                missileStartTime = System.nanoTime();
            }

            if(player.getY() <= -60 || player.getY() >= 500) {
                System.out.println("小鸟飞出了地图！" + player.getY());
                try {
                    Thread.sleep(750);
                }

                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                finally {
                    player.setPlaying(false);
                }
            }


            for(int i = 0; i< rockets.size();i++)
            {

                rockets.get(i).update();

                if(collision(rockets.get(i),player))
                {
                    try {
                        Thread.sleep(750);
                    }

                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    finally {
                        rockets.remove(i);
                        player.setPlaying(false);
                    }
//                    break;
                }

                if(rockets.get(i).getX()<-100)
                {
                    rockets.remove(i);
                    break;
                }
            }
        }
        else{
            newGameCreated = false;
            if(!newGameCreated) {
                newGame();
            }
        }
    }
    public boolean collision(GameObject a, GameObject b)
    {
        if(Rect.intersects(a.getRectangle(), b.getRectangle()))
        {
            return true;
        }

        return false;
    }
    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
        final float scaleFactorX = getWidth()/(WIDTH*1.f);
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);

        if(canvas!=null) {
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);

            //draw missiles
            for(Rocket m: rockets)
            {
                m.draw(canvas);
            }

            if(player.getScore() > best) {
                final int currentScore = player.getScore();
                final int oldBest = (best + 100);
                if(currentScore < oldBest && best != 0)
                    drawMessage(canvas, "新纪录!");
            }

            drawText(canvas);
            canvas.restoreToCount(savedState);
        }
    }

    public void newGame()
    {
        if(player.getScore() > best) {
            try {
                best = player.getScore();
                prefManager.add(SharedPrefManager.PREF_BEST_SCORE, best);
            }

            catch (Exception e) {
                e.printStackTrace();
            }
        }

        rockets.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;

        player.resetDYA();
        player.resetScore();
        player.setY(HEIGHT/2);

        newGameCreated = true;
    }

    public void drawText(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("距离: " + player.getScore(), 10, HEIGHT - 10, paint);
        canvas.drawText("最高分数: " + best, WIDTH - 215, HEIGHT - 10, paint);

        if(!player.getPlaying())
        {
            Paint paint1 = new Paint();
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("ROCKET BIRD", WIDTH / 2 - 50, HEIGHT / 2, paint1);

            Paint paint2 = new Paint();
            paint2.setTextSize(20);
            paint2.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("按住屏幕任意一点上升", WIDTH/2-50, HEIGHT/2 + 30, paint2);
            canvas.drawText("松开下落", WIDTH/2-50, HEIGHT/2 + 60, paint2);
            canvas.drawText("张宇许稚皎组", WIDTH/2-50, HEIGHT/2 + 90, paint2);
            canvas.drawText("Android Studio课程设计", WIDTH/2-50, HEIGHT/2 + 120, paint2);
        }
    }

    public void drawMessage(Canvas canvas, String message) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(message, (WIDTH / 2) - 120, (HEIGHT / 2) - 207, paint);
    }
}