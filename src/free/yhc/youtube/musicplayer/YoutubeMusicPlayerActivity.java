package free.yhc.youtube.musicplayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import free.yhc.youtube.musicplayer.model.MPlayer;

public class YoutubeMusicPlayerActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((Button)findViewById(R.id.test)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer();
            }
        });
    }

    public void audioPlayer() {
        /*
        int size = 0;
        URL url = null;
        try {
            url = new URL(sUrl);
            URLConnection conn = url.openConnection();
            size = conn.getContentLength();
            if (size < 0)
                System.out.println("Could not determine file size.");
            else
                System.out.println(sUrl + "\nSize: " + size);
            conn.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
         */
        String sUrl = "rtsp://v5.cache3.c.youtube.com/CjgLENy73wIaLwkDo0IGWhJYUxMYESARFEIUeXRhcGkteW91dHViZS1zZWFyY2hIBlIGdmlkZW9zDA==/0/0/0/video.3gp";
        MPlayer mp = new MPlayer(this);
        mp.init(findViewById(R.id.test), sUrl);
        mp.start();
    }
}