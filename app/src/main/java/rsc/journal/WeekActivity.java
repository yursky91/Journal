package rsc.journal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("ConstantConditions")

public class WeekActivity extends AppCompatActivity {

    LinearLayout worksLayout;
    TabLayout weekHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week);
        setTitle("Оперативный план");

        worksLayout = findViewById(R.id.worksLayout);
        weekHeader = findViewById(R.id.weekHeader);

        //Select day tab
        weekHeader.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int selectTab = weekHeader.getSelectedTabPosition();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("tabNumber", selectTab);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(weekHeader.getTabAt(0));
            }
        });

        refreshWeekHeader(0);

        // Adding works
        for (String[] lineData : DayActivity.dataWorks) {
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
            Date dateStart = null;
            Date dateEnd = null;
            try {
                dateStart = df.parse(lineData[0]);
                dateEnd = df.parse(lineData[1]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(DayActivity.dateSelect);
            calendar.set(Calendar.DAY_OF_WEEK, 2);      //monday
            Date dateMonday = calendar.getTime();
            calendar.set(Calendar.DAY_OF_WEEK, 8);      //sunday
            calendar.set(Calendar.HOUR_OF_DAY, 24);
            Date dateSunday = calendar.getTime();

            if ((dateStart.getTime() >= dateMonday.getTime()) && (dateStart.getTime() <= dateSunday.getTime())) {
                FrameLayout frameLayout = null;
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int parentWidth = displayMetrics.widthPixels;
                int frameWidth = Math.round(parentWidth * 0.7f);

                // Check if work already exists
                for (int i = 0; i < worksLayout.getChildCount(); i++) {
                    View view = worksLayout.getChildAt(i);
                    if (view.getTag() != null) {
                        if ((view.getTag().toString()).equals(lineData[2])) {

                            LinearLayout row = (LinearLayout) view;
                            for (int k = 0; k < row.getChildCount(); k++) {
                                View view1 = row.getChildAt(k);
                                if (view1 instanceof FrameLayout) frameLayout = (FrameLayout) view1;
                            }
                            break;
                        }
                    }
                }

                if (frameLayout == null) {
                    // Add a work row
                    LinearLayout rowLayout = new LinearLayout(this);
                    rowLayout.setTag(lineData[2]);
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);

                    // Defining the layout parameters of the RowLayout
                    LinearLayout.LayoutParams rllp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(25));

                    // Creating a new TextView
                    TextView tv = new TextView(this);
                    tv.setText(lineData[2]);
                    tv.setSingleLine(true);
                    tv.setGravity(Gravity.START | Gravity.CENTER);

                    // Defining the layout parameters of the TextView
                    TableRow.LayoutParams tvlp = new TableRow.LayoutParams(
                            0,
                            TabLayout.LayoutParams.MATCH_PARENT,
                            3);

                    // Creating a new FrameLayout
                    frameLayout = new FrameLayout(this);

                    // Defining the layout parameters of FrameLayout
                    TableRow.LayoutParams frlp = new TableRow.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            7);

                    // Setting the parameters on the TextView
                    tv.setLayoutParams(tvlp);
                    tv.setPadding(dpToPx(4),0,0,0);

                    // Setting the parameters on the FrameLayout
                    frameLayout.setLayoutParams(frlp);

                    // Setting the parameters on the RowLayout
                    rowLayout.setLayoutParams(rllp);

                    // Adding the views to the RowLayout and MainLayout
                    rowLayout.addView(tv);
                    rowLayout.addView(frameLayout);
                    worksLayout.addView(rowLayout);


                    // Adding days dividers
                    int dayWidth = frameWidth / 7;
                    int workDayWidth = Math.round(dayWidth / 2.66f);
                    int workDayStart = dayWidth / 3; //8 am

                    for (int k = 0; k < 7; k++) {
                        View line = new View(this);
                        line.setBackgroundColor(getResources().getColor(R.color.grey));
                        // Defining the layout parameters of the Line
                        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                                dpToPx(1),
                                LinearLayout.LayoutParams.MATCH_PARENT);

                        llp.setMargins(k * dayWidth, 0, 0, 0);
                        line.setLayoutParams(llp);

                        frameLayout.addView(line);

                        line = new View(this);
                        line.setBackgroundColor(getResources().getColor(R.color.grey_tr));
                        // Defining the layout parameters of the workTime
                        llp = new FrameLayout.LayoutParams(
                                workDayWidth,
                                dpToPx(25));

                        llp.setMargins(k * dayWidth + workDayStart, 0, 0, 0);
                        line.setLayoutParams(llp);

                        frameLayout.addView(line);
                    }

                    // Adding horizontal line
                    View line = new View(this);
                    line.setBackgroundColor(getResources().getColor(R.color.grey));
                    // Defining the layout parameters of the Line
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(1));

                    line.setLayoutParams(llp);
                    worksLayout.addView(line);
                }

                // Adding works
                long secWorkStart = (dateStart.getTime() - dateMonday.getTime()) / 1000;
                long secWorkTime = (dateEnd.getTime() - dateStart.getTime()) / 1000;

                float workStartPart = (float) secWorkStart / 604800;
                float workTimePart = (float) secWorkTime / 604800;
                int workStartPx = Math.round(frameWidth * workStartPart);
                int workTimePx = Math.round(frameWidth * workTimePart);


                // Adding WorkLine
                View line = new View(this);
                line.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                // Defining the layout parameters of the WorkLine
                FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                        workTimePx,
                        dpToPx(15));

                llp.setMargins(workStartPx, dpToPx(5), 0, 0);
                line.setLayoutParams(llp);
                frameLayout.addView(line);
            }
        }
    }

    void refreshWeekHeader(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DayActivity.dateSelect);
        calendar.add(Calendar.DATE, day);
        SimpleDateFormat df = new SimpleDateFormat("EEE\ndd", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            calendar.set(Calendar.DAY_OF_WEEK, i + 2);
            weekHeader.getTabAt(i).setText(df.format(calendar.getTime()));
        }
    }

    int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
