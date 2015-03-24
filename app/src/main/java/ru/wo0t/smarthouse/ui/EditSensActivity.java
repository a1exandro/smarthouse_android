package ru.wo0t.smarthouse.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;

public class EditSensActivity extends ActionBarActivity {
    private Sensor mSensor = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_sens);

        Intent intent = getIntent();
        int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
        AbstractBoard board = ((SMHZApp) getApplication()).getBoardsManager().getBoard(boardId);
        if (board != null) {
            Sensor sens = null;
            if (!intent.getStringExtra(boardsManager.SENSOR_NAME).isEmpty())        // edit sensor
                mSensor = board.getSens(intent.getStringExtra(boardsManager.SENSOR_NAME));
            Log.i(constants.APP_TAG, "edit sens: " + sens.getName());
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_sens, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
