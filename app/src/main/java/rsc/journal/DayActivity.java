package rsc.journal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("ConstantConditions")

public class DayActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    FrameLayout eventLayout;
    Button btnDate;
    Button btnOP;
    ImageButton btnNote;
    ImageButton btnAddEvent;
    ImageButton btnPhoto;
    ImageButton btnGallery;
    View timeLine;
    View timeLineEnd;
    View timeLineArrow;
    ScrollView eventScroll;
    TabLayout weekHeader;

    Runnable updater;
    Handler timerHandler;
    static ArrayList<String[]> dataEvents;
    static ArrayList<String[]> dataWorks;
    static ArrayList<String[]> dataNotes;
    ArrayList<String[]> dataImages;

    int parentHeight;
    int passedPx;
    boolean today;
    float x1, x2;
    static Date dateToday;
    static Date dateSelect;
    static String dirJournal = Environment.getExternalStorageDirectory() + "/Journal/";
    boolean manualTab = true;

    final int TYPE_PHOTO = 2;
    final int TYPE_VIDEO = 3;

    final int REQUEST_CODE_PHOTO = 2;
    final int REQUEST_CODE_VIDEO = 3;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        String permissions[] = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO};

        ActivityCompat.requestPermissions(this, permissions, 0);

        eventLayout = findViewById(R.id.eventLayout);
        btnDate = findViewById(R.id.btnDate);
        btnOP = findViewById(R.id.btnOP);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        timeLine = findViewById(R.id.timeLine);
        timeLineEnd = findViewById(R.id.timeLineEnd);
        timeLineArrow = findViewById(R.id.timeLineArrow);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnGallery = findViewById(R.id.btnGallery);
        weekHeader = findViewById(R.id.weekHeader);
        btnNote = findViewById(R.id.btnNote);

        parentHeight = eventLayout.getLayoutParams().height;

        Calendar calendar = Calendar.getInstance();
        dateToday = calendar.getTime();
        dateSelect = dateToday;
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy, EEE\nHH:mm:ss", Locale.getDefault());
        btnDate.setText(df.format(dateToday));

        //Select a tab
        refreshWeekHeader(0);
        int openDay = calendar.get(Calendar.DAY_OF_WEEK);
        if (openDay > 1) weekHeader.getTabAt(openDay - 2).select();
        else weekHeader.getTabAt(6).select();

        //Swap tab
        eventScroll = findViewById(R.id.eventScroll);
        eventScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();

                        float deltaX = x2 - x1;

                        if (Math.abs(deltaX) > 200) {
                            int currentTab = weekHeader.getSelectedTabPosition();
                            if (deltaX > 0) {
                                if (currentTab > 0) weekHeader.getTabAt(currentTab - 1).select();
                                else {
                                    refreshWeekHeader(-3);
                                    weekHeader.getTabAt(6).select();
                                }
                                return true;
                            } else {
                                if (currentTab < 6) weekHeader.getTabAt(currentTab + 1).select();
                                else {
                                    refreshWeekHeader(3);
                                    weekHeader.getTabAt(0).select();
                                }
                                return true;
                            }
                        }
                        break;
                }
                return false;
            }
        });

        //Change tab
        weekHeader.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            int lastTab = weekHeader.getSelectedTabPosition();
            String lastTabText = weekHeader.getTabAt(lastTab).getText().toString();

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (manualTab) {
                    int newTab = tab.getPosition();

                    if (weekHeader.getTabAt(lastTab).getText().toString().equals(lastTabText)) {    //check if week had been changed
                        changeDay(newTab - lastTab);
                    } else if (newTab > lastTab) changeDay(-1);
                    else changeDay(1);
                }

                lastTab = tab.getPosition();
                lastTabText = tab.getText().toString();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        btnAddEvent.setOnClickListener(this);
        btnGallery.setOnClickListener(this);
        btnPhoto.setOnClickListener(this);
        btnDate.setOnClickListener(this);
        btnOP.setOnClickListener(this);
        btnNote.setOnClickListener(this);

        updateTime();

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                eventScroll.smoothScrollTo(0, passedPx - eventScroll.getHeight() / 2);
            }
        }, 250); // 250 ms delay
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            Toast.makeText(this, "Карта памяти недоступна", Toast.LENGTH_SHORT).show();
        } else {
            readData("events");
            readData("works");
            readData("notes");
            readData("gallery");
            changeDay(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updater);
    }

    void updateTime() {
        updater = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                dateToday = calendar.getTime();

                //Time box
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String time = df.format(dateToday);
                df = new SimpleDateFormat("dd.MM.yy, EEE", Locale.getDefault());
                String date = df.format(dateSelect);
                String dateTime = date + "\n" + time;
                btnDate.setText(dateTime);

                //Timeline
                long now = calendar.getTimeInMillis();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long millsPassed = now - calendar.getTimeInMillis();
                long secondsPassed = millsPassed / 1000;

                float passedPart = (float) secondsPassed / 86400;
                passedPx = Math.round(parentHeight * passedPart);

                if (today) {
                    eventLayout.requestLayout();
                    timeLine.getLayoutParams().height = passedPx;
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) timeLineEnd.getLayoutParams();
                    lp.setMargins(dpToPx(50), passedPx, 0, 0);

                    lp = (FrameLayout.LayoutParams) timeLineArrow.getLayoutParams();
                    lp.setMargins(dpToPx(40), passedPx - dpToPx(4), 0, 0);
                }

                timerHandler.postDelayed(updater, 1000);
            }
        };

        timerHandler = new Handler();
        timerHandler.post(updater);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void readData(String type) {

        if (type.equals("gallery")) {

            File camDir = new File(DayActivity.dirJournal + "Camera/");

            if (!camDir.exists()) camDir.mkdirs();

            File[] files = camDir.listFiles();

            dataImages = new ArrayList<>();
            for (File aFile : files) {
                String date = aFile.getName().substring(aFile.getName().indexOf("_") + 1).replace("-", ":");
                date = date.substring(0, date.length() - 3);
                dataImages.add(date.split("_"));
            }

            return;
        }

        File file = new File(dirJournal + type + ".txt");
        ArrayList<String> lines = new ArrayList<>();

        try {
            if (!file.exists()) {
                File fileDir = new File(dirJournal);
                fileDir.mkdirs();
                file.createNewFile();
            }

            FileReader fileReader = new FileReader(file);

            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (type) {
            case "events":
                dataEvents = new ArrayList<>();
                for (String line : lines) {
                    dataEvents.add(line.split("_"));
                }
                break;
            case "works":
                dataWorks = new ArrayList<>();
                for (String line : lines) {
                    dataWorks.add(line.split("_"));
                }
                break;
            case "notes":
                dataNotes = new ArrayList<>();
                for (String line : lines) {
                    dataNotes.add(line.split("_"));
                }
                break;
        }

        MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, null, null);
    }

    public void onClick(View v) {
        //Buttons control
        Intent intent;
        switch (v.getId()) {
            case R.id.btnAddEvent:
                intent = new Intent(this, NewEventActivity.class);
                startActivity(intent);
                //Toast.makeText(this, newEventDate.toString(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnPhoto:
                registerForContextMenu(btnPhoto);
                openContextMenu(btnPhoto);
                unregisterForContextMenu(btnPhoto);
                break;
            case R.id.btnGallery:
                intent = new Intent(this, GalleryActivity.class);
                startActivity(intent);
                break;
            case R.id.btnDate:
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateSelect);

                new DatePickerDialog(this, R.style.TimePickerTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar openDate = Calendar.getInstance();
                        openDate.set(year, monthOfYear, dayOfMonth);

                        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy, EEE\nHH:mm:ss", Locale.getDefault());
                        String date = df.format(openDate.getTime());
                        btnDate.setText(date);
                        changeDay(0);
                        refreshWeekHeader(0);

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(dateSelect);
                        int openDay = calendar.get(Calendar.DAY_OF_WEEK);
                        manualTab = false;
                        if (openDay > 1)
                            weekHeader.getTabAt(openDay - 2).select();
                        else weekHeader.getTabAt(6).select();
                        manualTab = true;
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE)).show();
                break;
            case R.id.btnOP:
                intent = new Intent(this, WeekActivity.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.btnNote:
                intent = new Intent(this, NoteListActivity.class);
                startActivity(intent);
                break;
        }

        // Save event state
        if (v instanceof CheckBox) {
            CheckBox chkBox = (CheckBox) v;

            for (int i = 0; i < dataEvents.size(); i++) {
                String date = dataEvents.get(i)[0] + "_" + dataEvents.get(i)[1];
                if (date.equals(v.getTag().toString())) {
                    if (chkBox.isChecked()) {
                        dataEvents.get(i)[3] = "1";
                        chkBox.setTextColor(getResources().getColor(R.color.grey));
                    } else {
                        chkBox.setTextColor(getResources().getColor(R.color.white));
                        dataEvents.get(i)[3] = "0";
                    }
                }
            }

            try {
                FileWriter fileWriter = new FileWriter(dirJournal + "events.txt");

                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                for (String[] lineData : dataEvents) {
                    bufferedWriter.write(lineData[0] + "_" + lineData[1] + "_" + lineData[2] + "_" + lineData[3] + "\r\n");
                }
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // WorkBox, NoteIcon, GalleryIcon
        if (v.getTag() != null) {
            if (v instanceof TextView) {
                //Edit work
                intent = new Intent(this, NewEventActivity.class);
                intent.putExtra("datePeriod", v.getTag().toString());
                intent.putExtra("work", ((TextView) v).getText().toString());
                startActivity(intent);
            } else if (v instanceof ImageButton) {
                if (v.getTag().toString().contains("notes")) {
                    intent = new Intent(this, NoteListActivity.class);
                    intent.putExtra("dateTime", v.getTag().toString().split("-")[0]);
                    startActivity(intent);
                } else {
                    intent = new Intent(this, GalleryActivity.class);
                    intent.putExtra("dateTime", v.getTag().toString().split("-")[0]);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof CheckBox) {
            //Edit event
            Intent intent = new Intent(this, NewEventActivity.class);
            intent.putExtra("dateTime", v.getTag().toString());
            intent.putExtra("event", ((CheckBox) v).getText().toString());
            startActivity(intent);
        }
        return false;
    }

    void changeDay(int day) {
        //Setting a new day
        int gridViews = 51;
        eventLayout.removeViews(gridViews, eventLayout.getChildCount() - gridViews);
        String dateSelectText = btnDate.getText().toString();

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy, EEE\nHH:mm:ss", Locale.getDefault());
        dateSelect = null;
        try {
            dateSelect = df.parse(dateSelectText);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateSelect);
        calendar.add(Calendar.DATE, day);
        btnDate.setText(df.format(calendar.getTime()));

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        dateSelect = calendar.getTime();

        today = false;
        timeLine.setVisibility(View.VISIBLE);
        timeLineEnd.setVisibility(View.INVISIBLE);
        timeLineArrow.setVisibility(View.INVISIBLE);
        if (DateUtils.isToday(calendar.getTimeInMillis())) {
            timeLine.getLayoutParams().height = passedPx;
            timeLineEnd.setVisibility(View.VISIBLE);
            timeLineArrow.setVisibility(View.VISIBLE);
            today = true;
        } else if (dateSelect.before(dateToday)) {
            timeLine.getLayoutParams().height = parentHeight;
        } else timeLine.setVisibility(View.INVISIBLE);


        //Create work list
        for (String[] lineData : dataWorks) {
            df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
            Date dateStart = null;
            Date dateEnd = null;
            try {
                dateStart = df.parse(lineData[0]);
                dateEnd = df.parse(lineData[1]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            calendar.setTime(dateStart);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date dayStart = calendar.getTime();

            if (dateSelect.equals(dayStart)) {
                long secToWork = (dateStart.getTime() - dateSelect.getTime()) / 1000;
                long secWorkTime = (dateEnd.getTime() - dateStart.getTime()) / 1000;

                float workStartPart = (float) secToWork / 86400;
                float workTimePart = (float) secWorkTime / 86400;
                int workStartPx = Math.round(parentHeight * workStartPart);
                int workTimePx = Math.round(parentHeight * workTimePart);

                // Adding WorkLine
                View workPlace = new View(this);
                workPlace.setBackgroundColor(getResources().getColor(R.color.grey_tr_dark));
                // Defining the layout parameters of the WorkBox
                FrameLayout.LayoutParams wplp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        workTimePx - dpToPx(1));

                wplp.setMargins(dpToPx(50), workStartPx + dpToPx(1), 0, 0);
                workPlace.setTag(df.format(dateStart) + "_" + df.format(dateEnd));    // for can find a line in a file
                eventLayout.addView(workPlace, wplp);

                // Adding WorkBox
                TextView workBox = new TextView(this);
                workBox.setBackground(getResources().getDrawable(R.drawable.my_button_bg1));
                workBox.setPadding(2, 0, 0, 0);

                // Defining the layout parameters of the WorkBox
                FrameLayout.LayoutParams wllp = new FrameLayout.LayoutParams(
                        //workTimePx - dpToPx(5),
                        workTimePx - dpToPx(1),
                        dpToPx(30),
                        Gravity.END);

                //wllp.setMargins(0, ((workStartPx+dpToPx(3)) + ((workTimePx-dpToPx(5)) - dpToPx(30)) / 2), -((workTimePx-dpToPx(5)) - dpToPx(30)) / 2, 0);  //because of rotation margins is complex
                wllp.setMargins(0, ((workStartPx + dpToPx(1)) + ((workTimePx - dpToPx(1)) - dpToPx(30)) / 2), -((workTimePx - dpToPx(1)) - dpToPx(30)) / 2, 0);  //because of rotation margins is complex
                workBox.setRotation(90);
                workBox.setText(lineData[2]);
                workBox.setSingleLine(true);
                workBox.setGravity(Gravity.CENTER_VERTICAL);
                workBox.setTag(df.format(dateStart) + "_" + df.format(dateEnd));    // for can find a line in a file
                eventLayout.addView(workBox, wllp);

                workBox.setOnClickListener(this);
            }
        }

        //Create event list
        for (String[] lineData : dataEvents) {
            df = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
            Date dateEvent = null;
            try {
                dateEvent = df.parse(lineData[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (dateSelect.equals(dateEvent)) {
                df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());

                try {
                    dateEvent = df.parse(lineData[0] + "_" + lineData[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                calendar.setTime(dateSelect);
                long dateSelectMills = calendar.getTimeInMillis();
                calendar.setTime(dateEvent);
                long dateEventMills = calendar.getTimeInMillis();
                long millsToEvent = dateEventMills - dateSelectMills;
                long secToEvent = millsToEvent / 1000;

                float eventPart = (float) secToEvent / 86400;
                int eventPx = Math.round(parentHeight * eventPart);

                FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(20));
                lParams.setMargins(dpToPx(60), eventPx, dpToPx(30), 0);
                CheckBox chkNew = new CheckBox(this);
                chkNew.setText(lineData[2]);
                chkNew.setSingleLine(true);
                chkNew.setTag(df.format(dateEvent));    // for can find a line in a file
                if (lineData[3].equals("1")) {
                    chkNew.setChecked(true);
                    chkNew.setTextColor(getResources().getColor(R.color.grey));
                }

                eventLayout.addView(chkNew, lParams);

                chkNew.setOnClickListener(this);
                chkNew.setOnLongClickListener(this);
            }
        }

        boolean[] noteHour = new boolean[24];

        //Create note icons (foreach, but will be better make for one in hour on future)
        for (String[] lineData : dataNotes) {
            df = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
            Date dateNote = null;
            try {
                dateNote = df.parse(lineData[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (dateSelect.equals(dateNote)) {
                df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());

                try {
                    dateNote = df.parse(lineData[0] + "_" + lineData[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                calendar.setTime(dateSelect);
                long dateSelectMills = calendar.getTimeInMillis();
                calendar.setTime(dateNote);
                calendar.set(Calendar.MINUTE, 10);
                long dateNoteMills = calendar.getTimeInMillis();
                long millsToNote = dateNoteMills - dateSelectMills;
                long secToNote = millsToNote / 1000;

                float notePart = (float) secToNote / 86400;
                int notePx = Math.round(parentHeight * notePart);

                FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                        dpToPx(50), dpToPx(50));
                lParams.setMargins(0, notePx, 0, 0);
                ImageButton noteBtn = new ImageButton(this);
                noteBtn.setPadding(dpToPx(13), dpToPx(13), dpToPx(13), dpToPx(13));
                noteBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
                noteBtn.setImageResource(R.drawable.note);
                noteBtn.setBackground(getResources().getDrawable(R.drawable.my_button_bg_tr));
                noteBtn.setTag(df.format(dateNote) + "-notes");    // for can find a line in a file

                eventLayout.addView(noteBtn, lParams);

                noteBtn.setOnClickListener(this);

                noteHour[calendar.get(Calendar.HOUR_OF_DAY)] = true; // for right position of galleryIcon
            }
        }

        //Create gallery icons (foreach, but will be better make for one in hour on future)
        for (String[] lineData : dataImages) {
            df = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
            Date dateImage = null;
            try {
                dateImage = df.parse(lineData[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (dateSelect.equals(dateImage)) {
                df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());

                try {
                    dateImage = df.parse(lineData[0] + "_" + lineData[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                calendar.setTime(dateSelect);
                long dateSelectMills = calendar.getTimeInMillis();
                calendar.setTime(dateImage);

                if (noteHour[calendar.get(Calendar.HOUR_OF_DAY)])
                    calendar.set(Calendar.MINUTE, 35);
                else
                    calendar.set(Calendar.MINUTE, 10);

                long dateImageMills = calendar.getTimeInMillis();
                long millsToImage = dateImageMills - dateSelectMills;
                long secToImage = millsToImage / 1000;

                float imagePart = (float) secToImage / 86400;
                int imagePx = Math.round(parentHeight * imagePart);

                FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                        dpToPx(50), dpToPx(50));
                lParams.setMargins(0, imagePx, 0, 0);
                ImageButton imageBtn = new ImageButton(this);
                imageBtn.setPadding(dpToPx(13), dpToPx(13), dpToPx(13), dpToPx(13));
                imageBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageBtn.setImageResource(R.drawable.gallery);
                imageBtn.setBackground(getResources().getDrawable(R.drawable.my_button_bg_tr));
                imageBtn.setTag(df.format(dateImage) + "-images");    // for can find a line in a file

                eventLayout.addView(imageBtn, lParams);

                imageBtn.setOnClickListener(this);
            }
        }
    }

    void refreshWeekHeader(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateSelect);
        calendar.add(Calendar.DATE, day);
        SimpleDateFormat df = new SimpleDateFormat("EEE\ndd", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i + 2);
            weekHeader.getTabAt(i).setText(df.format(calendar.getTime()));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1: {
                if (resultCode == Activity.RESULT_OK) {
                    int newTab = data.getIntExtra("tabNumber", 0);
                    weekHeader.getTabAt(newTab).select();
                }
                break;
            }
            case REQUEST_CODE_PHOTO:
            case REQUEST_CODE_VIDEO: {
                if (resultCode == RESULT_OK)
                    Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
                else if (resultCode == RESULT_CANCELED)
                    Toast.makeText(this, "Отменено", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }


    //Camera
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.btnPhoto:
                menu.add(0, 1, 0, "Фото");
                menu.add(0, 2, 0, "Видео");
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        StrictMode.VmPolicy.Builder builder;
        Intent intent;
        switch (item.getItemId()) {
            // пункты меню для tvColor
            case 1:
                //To avoid error
                builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, generateFileUri(TYPE_PHOTO));
                startActivityForResult(intent, REQUEST_CODE_PHOTO);
                break;
            case 2:
                //To avoid error
                builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, generateFileUri(TYPE_VIDEO));
                startActivityForResult(intent, REQUEST_CODE_VIDEO);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private Uri generateFileUri(int type) {
        File file = null;

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy_HH:mm:ss", Locale.getDefault());
        String time = df.format(System.currentTimeMillis());

        switch (type) {
            case TYPE_PHOTO:
                file = new File(dirJournal + "Camera/" + "photo_"
                        + time.replace(":", "-") + ".jpg");
                break;
            case TYPE_VIDEO:
                file = new File(dirJournal + "Camera/" + "video_"
                        + time.replace(":", "-") + ".mp4");
                break;
        }

        MediaScannerConnection.scanFile(this, new String[]{dirJournal + "Camera/"}, null, null);

        return Uri.fromFile(file);
    }

    int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            Toast.makeText(this, "Карта памяти недоступна", Toast.LENGTH_SHORT).show();
    }
}
