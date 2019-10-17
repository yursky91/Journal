package rsc.journal;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import rsc.journal.Gallery.CreateList;
import rsc.journal.Gallery.MyAdapter;

public class GalleryActivity extends AppCompatActivity implements View.OnCreateContextMenuListener {

    ArrayList<CreateList> createLists;
    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        setTitle("Галерея");

        RecyclerView imagegallery = findViewById(R.id.imagegallery);
        imagegallery.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 2);
        imagegallery.setLayoutManager(layoutManager);

        createLists = prepareData();
        Collections.reverse(createLists);
        adapter = new MyAdapter(getApplicationContext(), createLists);
        imagegallery.setAdapter(adapter);
    }

    private ArrayList<CreateList> prepareData() {

        ArrayList<CreateList> theimage = new ArrayList<>();

        String path = DayActivity.dirJournal + "Camera/";
        File[] file = new File(path).listFiles();
        for (File aFile : file) {
            CreateList createList = new CreateList();
            createList.setImage_Location(aFile.getPath());
            createList.setImage_title(aFile.getName().substring(aFile.getName().indexOf("_") + 1).replace("-", ":"));
            theimage.add(createList);
        }

        //If specific hour check
        ArrayList<CreateList> dataImagesDate = new ArrayList<>();

        if (getIntent().hasExtra("dateTime")) {

            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());
            Date dateImage = null;
            try {
                dateImage = df.parse(getIntent().getStringExtra("dateTime"));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateImage);
            calendar.set(Calendar.MINUTE, 0);
            Date start = calendar.getTime();
            calendar.add(Calendar.HOUR, 1);
            Date end = calendar.getTime();

            for (CreateList image : theimage) {
                dateImage = null;
                try {
                    String title = image.getImage_title();
                    title = title.substring(0, title.length() - 3);
                    dateImage = df.parse(title);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if ((dateImage.equals(start) || dateImage.after(start)) && dateImage.before(end)) {
                    dataImagesDate.add(image);
                }
            }
        } else
            dataImagesDate = theimage;


        return dataImagesDate;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // пункты меню
            case 1:
                // получаем инфу о выбранном фото и удаляем
                File file = new File(createLists.get(item.getGroupId()).getImage_Location());
                file.delete();
                createLists.remove(createLists.get(item.getGroupId()));
                // уведомляем, что данные изменились
                adapter.notifyDataSetChanged();
                break;
        }
        return super.onContextItemSelected(item);
    }
}
