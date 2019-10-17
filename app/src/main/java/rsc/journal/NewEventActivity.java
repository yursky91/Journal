package rsc.journal;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NewEventActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout eventList;
    LinearLayout main;
    AutoCompleteTextView editName;
    EditText editDate;
    EditText editTimeStart;
    EditText editTimeEnd;
    CheckBox workTrue;
    CheckBox chkAlarm;
    Button btnAdd;
    Button btnDel;
    TextView textAdd;

    String event;
    String work;
    String date;
    String timeStart;
    String timeEnd;
    String dateTime;
    String[] datePeriod;
    ArrayList<String[]> eventArray;
    ArrayAdapter<String> adapter;
    boolean newOne = true;
    boolean alarmExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        setTitle("Добавить задачу");

        eventList = findViewById(R.id.eventList);
        main = findViewById(R.id.main);
        editName = findViewById(R.id.editName);
        editDate = findViewById(R.id.editDate);
        editTimeStart = findViewById(R.id.editTimeStart);
        editTimeEnd = findViewById(R.id.editTimeEnd);
        workTrue = findViewById(R.id.workTrue);
        btnAdd = findViewById(R.id.btnAdd);
        btnDel = findViewById(R.id.btnDel);
        textAdd = findViewById(R.id.textAdd);
        chkAlarm = findViewById(R.id.chkAlarm);

        editDate.setOnClickListener(this);
        editTimeStart.setOnClickListener(this);
        editTimeEnd.setOnClickListener(this);
        workTrue.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
        btnDel.setOnClickListener(this);
        chkAlarm.setOnClickListener(this);

        editName.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        date = new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(DayActivity.dateSelect);
        editDate.setText(date);
        btnDel.setVisibility(View.GONE);

        // create work choose list
        int size = DayActivity.dataWorks.size();
        String[] workList = new String[size];
        for (int i = 0; i < size; i++) {
            workList[i] = DayActivity.dataWorks.get(i)[2];
        }
        // remove duplicates
        workList = new HashSet<String>(Arrays.asList(workList)).toArray(new String[0]);
        //Create Array Adapter
        adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, workList);
        //Set the number of characters the user must type before the drop down list is shown
        editName.setThreshold(1);
        editName.setDropDownBackgroundResource(R.color.white);

        //Set time for exist event
        if (getIntent().hasExtra("dateTime")) {
            event = getIntent().getExtras().getString("event");
            dateTime = getIntent().getExtras().getString("dateTime");
            timeStart = dateTime.substring(dateTime.indexOf("_") + 1);

            editName.setText(event);
            editTimeStart.setText(timeStart);
            btnAdd.setText("Сохранить");
            btnDel.setVisibility(View.VISIBLE);
            workTrue.setVisibility(View.GONE);
            setTitle("Изменить задачу");
            newOne = false;
        } else if (getIntent().hasExtra("datePeriod")) {
            work = getIntent().getExtras().getString("work");

            datePeriod = getIntent().getExtras().getString("datePeriod").split("_");
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
            Date dateStart = null;
            Date dateEnd = null;
            try {
                dateStart = df.parse(datePeriod[0]);
                dateEnd = df.parse(datePeriod[1]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            timeStart = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateStart);
            timeEnd = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateEnd);

            editName.setText(work);
            editTimeEnd.setVisibility(View.VISIBLE);
            editTimeStart.setText(timeStart);
            editTimeEnd.setText(timeEnd);
            workTrue.setVisibility(View.GONE);
            workTrue.setChecked(true);
            btnAdd.setText("Сохранить");
            btnDel.setVisibility(View.VISIBLE);
            textAdd.setVisibility(View.VISIBLE);
            editName.setHint("Введите работу");
            setTitle("Изменить работу");
            newOne = false;

            //Set the adapter fot list of work
            editName.setAdapter(adapter);

            findInEvents(datePeriod);

            for (String[] event : eventArray)
                addEventLine(event[1], event[2]);

            addEventLine("", "");
        } else {
            //Show keyboard
            editName.requestFocus();
        }

        if (!newOne) {
            Intent intent = new Intent(this, MyReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Math.round(getOldDate().getTime() / 100000), intent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                chkAlarm.setChecked(true);
                alarmExists = true;
            } else {
                chkAlarm.setChecked(false);
                alarmExists = false;
            }
        }
    }

    public void showDatePicker() {
        int y, m, d;
        y = Integer.valueOf("20" + date.substring(6, 8));
        m = Integer.valueOf(date.substring(3, 5)) - 1;
        d = Integer.valueOf(date.substring(0, 2));

        new DatePickerDialog(this, R.style.TimePickerTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);

                date = new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.getTime());
                editDate.setText(date);
            }
        }, y, m, d).show();
    }

    public void showTimePicker(final TextView editTime) {
        int h, min;
        if (editTime.getText().toString().equals("")) {
            if (editTime.getHint().toString().equals(":") && timeStart != null) {
                h = Integer.valueOf(timeStart.substring(0, timeStart.indexOf(":")));
                min = Integer.valueOf(timeStart.substring(timeStart.indexOf(":") + 1));
            } else {
                h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                min = Calendar.getInstance().get(Calendar.MINUTE);
            }
        } else {
            String time = editTime.getText().toString();
            h = Integer.valueOf(time.substring(0, time.indexOf(":")));
            min = Integer.valueOf(time.substring(time.indexOf(":") + 1));
        }

        new TimePickerDialog(this, R.style.TimePickerTheme, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime());
                editTime.setText(time);

                if (editTime.equals(editTimeStart)) timeStart = time;
                else if (editTime.equals(editTimeEnd)) timeEnd = time;
            }
        }, h, min, true).show();
    }

    @Override
    public void onClick(View v) {
        main.requestFocus();
        View view;
        switch (v.getId()) {
            case R.id.editDate:
                showDatePicker();
                break;
            case R.id.editTimeStart:
                showTimePicker(editTimeStart);
                break;
            case R.id.editTimeEnd:
                showTimePicker(editTimeEnd);
                break;
            case R.id.workTrue:
                if (workTrue.isChecked()) {
                    editTimeEnd.setVisibility(View.VISIBLE);
                    textAdd.setVisibility(View.VISIBLE);
                    setTitle("Добавить работу");
                    editName.setHint("Введите работу");
                    addEventLine("", "");

                    //Set the adapter fot list of work
                    editName.setAdapter(adapter);
                } else {
                    editTimeEnd.setVisibility(View.INVISIBLE);
                    editTimeEnd.setText("");
                    textAdd.setVisibility(View.INVISIBLE);
                    timeEnd = null;
                    setTitle("Добавить задачу");
                    editName.setHint("Введите задачу");
                    eventList.removeAllViews();

                    editName.setAdapter(null);
                }
                break;
            case R.id.btnAdd:
                addLineData();
                break;
            case R.id.btnDel:
                if (getIntent().hasExtra("dateTime")) RemoveEvent(dateTime);
                else RemoveWork(datePeriod);

                if (alarmExists) SettingAlarm(getOldDate().getTime(), false);
                finish();
                break;
        }

        if (v.getTag() != null && v.getTag().equals("time")) showTimePicker((TextView) v);
    }

    void addLineData() {

        if (editName.length() == 0 || editDate.length() == 0 || editTimeStart.length() == 0) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!workTrue.isChecked()) {
            //Add event
            if (!newOne) RemoveEvent(dateTime);
            event = editName.getText().toString();
            writeNewLine(date + "_" + timeStart + "_" + event + "_" + "0" + "\r\n", "events");

        } else {
            //Check work
            if (editTimeEnd.length() == 0) {
                Toast.makeText(this, "Укажите время завершения", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date timeStartDate = null;
            Date timeEndDate = null;
            try {
                timeStartDate = df.parse(timeStart);
                timeEndDate = df.parse(timeEnd);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (timeStartDate.after(timeEndDate) || timeStartDate.equals(timeEndDate)) {
                Toast.makeText(this, "Неверное время завершения", Toast.LENGTH_SHORT).show();
                return;
            }

            int count = eventList.getChildCount();
            for (int i = 0; i < count; i++) {
                View line = eventList.getChildAt(i);

                int count1 = ((ViewGroup) line).getChildCount();
                String eventW = null, time = null;
                for (int k = 0; k < count1; k++) {
                    View v = ((ViewGroup) line).getChildAt(k);
                    if (v instanceof EditText && v.getTag() == "time")
                        time = ((EditText) v).getText().toString();
                    else
                        eventW = ((EditText) v).getText().toString();
                }

                //Check new included events
                if (!eventW.equals("")) {
                    if (!time.equals("")) {
                        df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
                        Date dateStart = null;
                        Date dateEnd = null;
                        Date dateEvent = null;
                        try {
                            dateStart = df.parse(date + " " + timeStart);
                            dateEnd = df.parse(date + " " + timeEnd);
                            dateEvent = df.parse(date + " " + time);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if (!((dateEvent.after(dateStart) && dateEvent.before(dateEnd)) || dateEvent.equals(dateStart))) {
                            Toast.makeText(this, "Время задач вне диапазона", Toast.LENGTH_SHORT).show();
                            return;
                        }

                    } else {
                        Toast.makeText(this, "Укажите время для задач", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            //Add work
            if (!newOne) RemoveWork(datePeriod);
            work = editName.getText().toString();
            writeNewLine(date + " " + timeStart + "_" + date + " " + timeEnd + "_" + work + "\r\n", "works");

            //Add new included events
            for (int i = 0; i < count; i++) {
                View line = eventList.getChildAt(i);

                int count1 = ((ViewGroup) line).getChildCount();
                String eventW = null, time = null;
                for (int k = 0; k < count1; k++) {
                    View v = ((ViewGroup) line).getChildAt(k);
                    if (v instanceof EditText && v.getTag() == "time")
                        time = ((EditText) v).getText().toString();
                    else
                        eventW = ((EditText) v).getText().toString();
                }
                if (!eventW.equals(""))
                    writeNewLine(date + "_" + time + "_" + eventW + "_" + "0" + "\r\n", "events");
            }
        }

        if (alarmExists) SettingAlarm(getOldDate().getTime(), false);

        if (chkAlarm.isChecked()) {
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());
            Date newDate = null;
            try {
                newDate = df.parse(date + "_" + timeStart);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            SettingAlarm(newDate.getTime(), true);
        }

        finish();
    }

    void writeNewLine(String dataLine, String file) {
        try {
            FileWriter fileWriter = new FileWriter(DayActivity.dirJournal + file + ".txt", true);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(dataLine);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void RemoveEvent(String eventDate) {
        //Removing old event
        for (int i = 0; i < DayActivity.dataEvents.size(); i++) {
            String date = DayActivity.dataEvents.get(i)[0] + "_" + DayActivity.dataEvents.get(i)[1];
            if (date.equals(eventDate)) {
                DayActivity.dataEvents.remove(i);
                i--; //because array rewrite, so it skip a line
            }
        }
        //Removing from data file
        try {
            FileWriter fileWriter = new FileWriter(DayActivity.dirJournal + "events.txt");

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String[] lineData : DayActivity.dataEvents) {
                bufferedWriter.write(lineData[0] + "_" + lineData[1] + "_" + lineData[2] + "_" + lineData[3] + "\r\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void RemoveWork(String[] workPeriod) {
        //Removing old work
        for (int i = 0; i < DayActivity.dataWorks.size(); i++) {
            String date = DayActivity.dataWorks.get(i)[0];
            if (date.equals(workPeriod[0])) {
                DayActivity.dataWorks.remove(i);
                i--; //because array rewrite, so it skip a line
            }
        }
        //Removing from data file
        try {
            FileWriter fileWriter = new FileWriter(DayActivity.dirJournal + "works.txt");

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String[] lineData : DayActivity.dataWorks) {
                bufferedWriter.write(lineData[0] + "_" + lineData[1] + "_" + lineData[2] + "\r\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Detecting included events
        findInEvents(datePeriod);

        //Removing included events
        for (String[] lineData : eventArray)
            RemoveEvent(lineData[0] + "_" + lineData[1]);
    }

    void findInEvents(String[] workPeriod) {
        //Detecting included events
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
        Date dateStart = null;
        Date dateEnd = null;
        try {
            dateStart = df.parse(workPeriod[0]);
            dateEnd = df.parse(workPeriod[1]);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        eventArray = new ArrayList<>();
        df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());

        for (String[] lineData : DayActivity.dataEvents) {
            String date = lineData[0] + "_" + lineData[1];
            Date dateEvent = null;
            try {
                dateEvent = df.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if ((dateEvent.after(dateStart) && dateEvent.before(dateEnd)) || dateEvent.equals(dateStart)) {
                eventArray.add((lineData[0] + "_" + lineData[1] + "_" + lineData[2] + "_" + lineData[3]).split("_"));
            }
        }
    }

    void addEventLine(String t, String e) {
        // Add a event row
        LinearLayout rowLayout = new LinearLayout(this);
        //rowLayout.setTag(lineData[2]);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Defining the layout parameters of the RowLayout
        LinearLayout.LayoutParams rllp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Creating a new EditText for time
        EditText time = new EditText(this);
        time.setTag("time");
        time.setHint(":");
        time.setFocusable(false);
        time.setSingleLine(true);
        time.setGravity(Gravity.CENTER);
        time.setText(t);

        // Defining the layout parameters of the TextView
        LinearLayout.LayoutParams timelp = new LinearLayout.LayoutParams(
                dpToPx(80),
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Creating a new EditText for event
        final EditText event = new EditText(this);
        event.setHint("Введите задачу");
        event.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        event.setSingleLine(true);
        event.setImeOptions(EditorInfo.IME_ACTION_DONE);
        event.setGravity(Gravity.CENTER | Gravity.START);
        event.setText(e);

        // Defining the layout parameters of EditText
        LinearLayout.LayoutParams eventlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Setting the parameters on the time
        time.setLayoutParams(timelp);

        // Setting the parameters on the event
        event.setLayoutParams(eventlp);

        // Setting the parameters on the RowLayout
        rowLayout.setLayoutParams(rllp);

        // Adding the views to the RowLayout and MainLayout
        rowLayout.addView(time);
        rowLayout.addView(event);
        eventList.addView(rowLayout);

        time.setOnClickListener(this);

        event.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    v.clearFocus();
                }
                return false;
            }
        });

        event.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    main.requestFocus();
                    int z = 0;
                    int count = eventList.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View child = eventList.getChildAt(i);

                        int count1 = ((ViewGroup) child).getChildCount();
                        for (int k = 0; k < count1; k++) {
                            View child1 = ((ViewGroup) child).getChildAt(k);
                            if (child1 instanceof EditText && child1.getTag() == null && ((EditText) child1).getText().toString().equals(""))
                                z++;
                        }
                    }

                    if (z > 1) eventList.removeView((ViewGroup) v.getParent());
                    else if (z == 0) addEventLine("", "");
                }
            }
        });
    }

    public void SettingAlarm(long time, boolean write) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MyReceiver.class);
        intent.putExtra("data", editName.getText().toString());

        if (write) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Math.round(time / 100000), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            Toast.makeText(this, "Напоминание установлено", Toast.LENGTH_SHORT).show();
        } else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, Math.round(time / 100000), intent, PendingIntent.FLAG_NO_CREATE);
            am.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }


    Date getOldDate() {
        SimpleDateFormat df;
        Date oldDate = null;
        if (getIntent().hasExtra("dateTime")) {
            df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());

            try {
                oldDate = df.parse(dateTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            df = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());

            try {
                oldDate = df.parse(datePeriod[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return oldDate;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
