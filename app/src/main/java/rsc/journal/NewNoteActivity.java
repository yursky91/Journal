package rsc.journal;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NewNoteActivity extends AppCompatActivity {

    Button btnSaveNote;
    Button btnDelNote;
    EditText editNote;

    String note;
    String dateTime;

    boolean newOne = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
        setTitle("Добавить заметку");

        editNote = findViewById(R.id.editNote);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        btnDelNote = findViewById(R.id.btnDelNote);


        if (getIntent().hasExtra("note")) {
            setTitle("Изменить заметку");
            note = getIntent().getExtras().getString("note");
            dateTime = getIntent().getExtras().getString("dateTime");
            editNote.setText(note);

            btnSaveNote.setText("Сохранить");
            newOne = false;
        } else {
            btnDelNote.setVisibility(View.GONE);

            //Show keyboard
            editNote.requestFocus();
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSaveNote:
                if (editNote.getText().length() > 0) {
                    if (!newOne) RemoveNote(dateTime + "_" + note.replace(System.lineSeparator(), "|||"));
                    note = editNote.getText().toString().replace(System.lineSeparator(), "|||");

                    SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy_HH:mm", Locale.getDefault());
                    String date = df.format(DayActivity.dateToday);
                    writeNewLine(date + "_" + note + "\r\n", "notes");
                }
                finish();
                break;
            case R.id.btnDelNote:
                //Toast.makeText(this, note + "____" + note.replace(System.lineSeparator(), "|||"), Toast.LENGTH_LONG).show();

                RemoveNote(dateTime + "_" + note.replace(System.lineSeparator(), "|||"));
                finish();
                break;
        }
    }

     void RemoveNote(String noteDateTime) {
        //Removing old note
        for (int i = 0; i < DayActivity.dataNotes.size(); i++) {
            String dateNote = DayActivity.dataNotes.get(i)[0] + "_" + DayActivity.dataNotes.get(i)[1] + "_" + DayActivity.dataNotes.get(i)[2];
            if (dateNote.equals(noteDateTime)) {
                DayActivity.dataNotes.remove(i);
                i--; //because array rewrite, so it skip a line
            }
        }
        //Removing from data file
        try {
            FileWriter fileWriter = new FileWriter(DayActivity.dirJournal + "notes.txt");

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String[] lineData : DayActivity.dataNotes) {
                bufferedWriter.write(lineData[0] + "_" + lineData[1] + "_" + lineData[2] + "\r\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
