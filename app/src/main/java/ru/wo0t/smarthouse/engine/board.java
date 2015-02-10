package ru.wo0t.smarthouse.engine;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */

abstract public class board {
    protected static final String SYSTEM_SENSORS = "sensors";
    protected static final String SYSTEM_SWITCHES = "switches";
    protected static final String SYSTEM_SWITCHES2= "gpio";
    protected static final String SYSTEM_CAME= "camera";
    protected static final String SENSORS_CFG_REQ = SYSTEM_SENSORS+" get cfg;";
    protected static final String SWITCHES_CFG_REQ = SYSTEM_SWITCHES+" get cfg;";
    protected static final String CAME_CFG_REQ = SYSTEM_CAME+" get cfg;";

    public static enum BOARD_TYPE {LOCAL, REMOTE}

    protected BOARD_TYPE mBoardType;
    protected List<sensor>mSensors;
    protected String mBoardName;
    protected int mBoardId;
    protected JSONObject mConfig;

    public board(BOARD_TYPE type, int id) {
        mBoardType = type;
        mBoardId = id;
        mSensors = new ArrayList<sensor>();
    }

    public sensor getSens(String name) {
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getName().equals(name))
                return mSensors.get(i);
        return null;
    }

    public sensor getSens(int ind) {
        return mSensors.get(ind);
    }

    public int getSensNum() {
        return mSensors.size();
    }

    public void addSens(sensor sens) {
        mSensors.add(sens);
    }

    public List<sensor> getSensors() {
        return mSensors;
    }

    public boolean replaceSens(String sensName, sensor sens) {
        return replaceSens(getSens(sensName),sens);
    }

    public boolean replaceSens(sensor oldSens, sensor newSens) {
        boolean found = false;

        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i) == oldSens) {
                mSensors.set(i, newSens);
                found = true;
            }
        return found;
    }

    public List<sensor> getSensors(sensor.SENSOR_TYPE sType) {
        List<sensor> lst = new ArrayList();
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getType() == sType)
                lst.add(mSensors.get(i));
        return lst;
    }

    protected void messageParser(String msg) {
        String[] msgData = msg.split(":",2);
        if (msgData.length < 2) return;
        String system = msgData[0].trim().toLowerCase();
        try {
            JSONObject jData = new JSONObject(msgData[1].trim());
            if (system.equals(SYSTEM_SENSORS))
            {
                switch (jData.getString("type")) {
                    case "cfg":
                        JSONObject jDataSens = new JSONObject(jData.getString("data"));
                        JSONArray sensors = jDataSens.getJSONArray("sensors");
                        for (int i = 0; i < jData.length(); i++) {
                            JSONObject jSens = sensors.getJSONObject(i);
                            sensor sens = new sensor(jSens);
                            if (!replaceSens(sens.getName(),sens))
                                addSens(sens);
                        }
                        break;
                }
            }
        } catch (JSONException e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }
    abstract public void updateSens(String name);
}
