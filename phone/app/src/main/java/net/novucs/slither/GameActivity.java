package net.novucs.slither;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private Map<String, Player> players;
    private GameView renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

//        //noinspection unchecked,ConstantConditions
//        players = ((BinderWrapper<Map<String, Player>>) getIntent()
//                .getExtras().getBinder("players")).getData();
//
//        renderer = findViewById(R.id.game);
//        run();
//
//        System.out.println("SWITCHED TO GAME ACTIVITY");
//        for (String key : players.keySet()) {
//            System.out.println(key);
//        }
    }

    public void run() {
    }
}
