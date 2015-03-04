package ru.wo0t.smarthouse.board;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

abstract public class AbstractBoard {
    public static final String SYSTEM_SENSORS = "sensors";
    public static final String SYSTEM_SWITCHES = "switches";
    public static final String SYSTEM_CAME= "camera";
    protected static final String SENSORS_CFG_REQ = SYSTEM_SENSORS +" get cfg;";
    protected static final String SWITCHES_CFG_REQ = SYSTEM_SWITCHES +" get cfg;";
    protected static final String CAME_CFG_REQ = SYSTEM_CAME +" get cfg;";

    public static enum BOARD_TYPE {LOCAL, REMOTE}

    protected BOARD_TYPE mBoardType;
    protected List<Sensor>mSensors;
    protected String mBoardName;
    protected int mBoardId;
    Context mContext;

    public AbstractBoard(Context context, BOARD_TYPE type, int id, String name) {
        mBoardType = type;
        mBoardId = id;
        mSensors = new ArrayList<>();
        mContext = context;
        mBoardName = name;
    }

    public int getBoardId() { return mBoardId; }

    public Sensor getSens(String name) {
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getName().equals(name))
                return mSensors.get(i);
        return null;
    }

    public Sensor getSensByAddr(String addr) {
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getAddr().equals(addr))
                return mSensors.get(i);
        return null;
    }

    public Sensor getSens(int ind) {
        return mSensors.get(ind);
    }

    public int getSensNum() {
        return mSensors.size();
    }

    public void addSens(Sensor sens) {
        mSensors.add(sens);
    }

    public List<Sensor> getSensors() {
        return mSensors;
    }

    public boolean replaceSens(String sensAddr, Sensor sens) {
        return replaceSens(getSensByAddr(sensAddr),sens);
    }

    public boolean replaceSens(Sensor oldSens, Sensor newSens) {
        boolean found = false;

        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i) == oldSens) {
                mSensors.set(i, newSens);
                found = true;
            }
        return found;
    }

    public List<Sensor> getSensors(Sensor.SENSOR_TYPE sType) {
        List<Sensor> lst = new ArrayList<>();
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getType() == sType)
                lst.add(mSensors.get(i));
        return lst;
    }

    public List<Sensor> getSensors(Sensor.SENSOR_SYSTEM system) {
        List<Sensor> lst = new ArrayList<>();
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getSystem() == system)
                lst.add(mSensors.get(i));
        return lst;
    }

    protected void onBoardConnected() {
        sendBroadcastMsg(boardsManager.MSG_BOARD_CONNECTED);
        sendPkt(new String(SENSORS_CFG_REQ + SWITCHES_CFG_REQ + CAME_CFG_REQ).getBytes());
    }

    private void sendBroadcastMsg(String event) {
        sendBroadcastMsg(event, new Bundle());
    }

    private void sendBroadcastMsg(String event, Bundle args) {
        Intent intent = new Intent(event);

        args.putInt(boardsManager.BOARD_ID, mBoardId);
        args.putInt(boardsManager.BOARD_ID, mBoardType.ordinal());

        intent.putExtras(args);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    protected void messageParser(String msg) {
        String[] msgData = msg.split(":",2);
        if (msgData.length < 2) return;
        String system = msgData[0].trim().toLowerCase();

        Sensor.SENSOR_SYSTEM sensSystem;
        switch (system) // determine sensors type, based on system. for SYSTEM_SENSORS type will be determined by jSON field.
        {
            case SYSTEM_SENSORS: sensSystem = Sensor.SENSOR_SYSTEM.SENSES; break;
            case SYSTEM_CAME: sensSystem = Sensor.SENSOR_SYSTEM.CAMES; break;
            case SYSTEM_SWITCHES: sensSystem = Sensor.SENSOR_SYSTEM.SWITCHES; break;
            default: sensSystem = Sensor.SENSOR_SYSTEM.UNKNOWN; break;
        }

        try {
            JSONObject jData = new JSONObject(msgData[1].trim());

            switch (jData.getString("type")) {
                case "cfg":
                    JSONObject jDataSens = new JSONObject(jData.getString("data"));
                    JSONArray sensors = jDataSens.getJSONArray(system);
                    for (int i = 0; i < sensors.length(); i++) {
                        JSONObject jSens = sensors.getJSONObject(i);

                        Sensor sens = new Sensor(jSens,sensSystem);
                        if (!replaceSens(sens.getAddr(),sens))
                            addSens(sens);
                    }
                    onSystemCfgChanged(sensSystem);
                    Bundle args = new Bundle();
                    args.putString(boardsManager.MSG_SYSTEM_NAME, system);
                    sendBroadcastMsg(boardsManager.MSG_BOARD_CFG_CHANGED, args);
                    break;
                default:
                    if (jData.has("addr") && jData.has("data")) {
                        Sensor sens = getSensByAddr(jData.getString("addr"));
                        if (sens != null) {
                            sens.setVal(jData.getDouble("data"));

                            Bundle sensData = new Bundle();

                            sensData.putString(boardsManager.SENSOR_NAME, sens.getName());
                            sensData.putString(boardsManager.SENSOR_ADDR, sens.getAddr());
                            sensData.putInt(boardsManager.SENSOR_TYPE, sens.getType().ordinal());
                            sensData.putDouble(boardsManager.SENSOR_VALUE, (double)sens.getVal());
                            sensData.putString(boardsManager.MSG_SYSTEM_NAME, sensSystem.toString());

                            sendBroadcastMsg(boardsManager.MSG_SENSOR_DATA, sensData);
                        }
                    }
                    break;
                }

        } catch (JSONException e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    private void onSystemCfgChanged(Sensor.SENSOR_SYSTEM system) {
        if (mBoardType == BOARD_TYPE.REMOTE) return;    // do not request sensors data, web interface will do it for us
        List<Sensor> sens = getSensors(system);
        for (int i = 0; i < sens.size(); i++) {
            Sensor s = sens.get(i);
            updateSens(s);
        }
    }

    abstract public void updateSens(Sensor sens);
    abstract public void onSensorAction(Sensor sens, Object param);
    abstract public void sendPkt(byte[] data);
    abstract public void close();
}
