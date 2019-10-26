package rsc.journal;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;


public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        PhotoView photoView = findViewById(R.id.photoView);

        Glide
                .with(this)
                .load(getIntent().getStringExtra("path"))
                .into(photoView);
    }
}
