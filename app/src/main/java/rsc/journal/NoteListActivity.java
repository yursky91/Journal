package rsc.journal;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NoteListActivity extends AppCompatActivity {

    ListView lvMain;
    Button btnAddNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        setTitle("Заметки");

        lvMain = findViewById(R.id.lvMain);
        btnAddNote = findViewById(R.id.btnAddNote);
    }

    @Override
    protected void onResume() {
        super.onResume();
        readData("notes");

        //If specific hour check
        ArrayList<String[]> dataNotesDate = new ArrayList<>();
        if (getIntent().hasExtra("dateTime")) {

            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());
            Date dateNote = null;
            try {
                dateNote = df.parse(getIntent().getStringExtra("dateTime"));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateNote);
            calendar.set(Calendar.MINUTE, 0);
            Date start = calendar.getTime();
            calendar.add(Calendar.HOUR, 1);
            Date end = calendar.getTime();

            for (String[] lineData : DayActivity.dataNotes){
                dateNote = null;
                try {
                    dateNote = df.parse(lineData[0] +"_" + lineData[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if ((dateNote.equals(start) || dateNote.after(start)) && dateNote.before(end)) {
                    dataNotesDate.add(lineData);
                }
            }

            btnAddNote.setVisibility(View.GONE);
        } else
            dataNotesDate = DayActivity.dataNotes;

        // упаковываем данные в понятную для адаптера структуру
        ArrayList<Map<String, String>> data = new ArrayList<>();
        Map<String, String> m;

        for (String[] lineData : dataNotesDate) {
            m = new HashMap<>();
            m.put("date", lineData[0]);
            m.put("time", lineData[1]);
            m.put("note", lineData[2].replace("|||", System.lineSeparator()));
            data.add(m);
        }
        Collections.reverse(data);

        // массив имен атрибутов, из которых будут читаться данные
        String[] from = {"date", "time", "note"};
        // массив ID View-компонентов, в которые будут вставлять данные
        int[] to = {R.id.date, R.id.time, R.id.note};
        // создаем адаптер
        SimpleAdapter sa = new SimpleAdapter(this, data, R.layout.my_list_item, from, to);

        lvMain.setAdapter(sa);

        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView note = view.findViewById(R.id.note);
                TextView date = view.findViewById(R.id.date);
                TextView time = view.findViewById(R.id.time);
                String noteTx = note.getText().toString();
                String dateTime = date.getText().toString() + "_" + time.getText().toString();

                Intent intent = new Intent(getApplication(), NewNoteActivity.class);
                intent.putExtra("note", noteTx);
                intent.putExtra("dateTime", dateTime);
                startActivity(intent);
            }
        });
    }

    public void onAddButtonClick(View view) {
        Intent intent = new Intent(getApplication(), NewNoteActivity.class);
        startActivity(intent);
    }

    void readData(String type) {
        File file = new File(DayActivity.dirJournal + "notes.txt");
        ArrayList<String> lines = new ArrayList<>();

        try {
            if (!file.exists()) {
                File fileDir = new File(DayActivity.dirJournal);
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

        DayActivity.dataNotes = new ArrayList<>();
        for (String line : lines) {
            DayActivity.dataNotes.add(line.split("_"));
        }

        MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, null, null);
    }
}
