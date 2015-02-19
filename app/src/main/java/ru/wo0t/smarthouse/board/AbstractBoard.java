package ru.wo0t.smarthouse.board;

import android.os.Handler;
import android.os.Message;
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
    protected static final String SENSORS_CFG_REQ = SYSTEM_SENSORS+" get cfg;";
    protected static final String SWITCHES_CFG_REQ = SYSTEM_SWITCHES+" get cfg;";
    protected static final String CAME_CFG_REQ = SYSTEM_CAME+" get cfg;";

    public static enum BOARD_TYPE {LOCAL, REMOTE}

    protected BOARD_TYPE mBoardType;
    protected List<sensor>mSensors;
    protected String mBoardName;
    protected int mBoardId;
    protected JSONObject mConfig;

    public AbstractBoard(BOARD_TYPE type, int id) {
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

    public sensor getSensByAddr(String addr) {
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getAddr().equals(addr))
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

    public boolean replaceSens(String sensAddr, sensor sens) {
        return replaceSens(getSensByAddr(sensAddr),sens);
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

    public List<sensor> getSensors(sensor.SENSOR_SYSTEM system) {
        List<sensor> lst = new ArrayList();
        for (int i=0; i<mSensors.size(); i++)
            if (mSensors.get(i).getSystem() == system)
                lst.add(mSensors.get(i));
        return lst;
    }

    protected final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case constants.MESSAGE_CONNECTED:
                    Log.i(constants.APP_TAG, msg.getData().getString(constants.MESSAGE_INFO));
                    sendPkt(new String(SENSORS_CFG_REQ + SWITCHES_CFG_REQ + CAME_CFG_REQ).getBytes());
                    break;
                case constants.MESSAGE_NEW_MSG:
                    byte[] data = msg.getData().getByteArray(constants.MESSAGE_DATA);
                    Log.i(constants.APP_TAG, msg.getData().getString(constants.MESSAGE_INFO) +" " + new String(data));
                    messageParser(new String(data));
                    break;

            }
        }
    };

    protected void messageParser(String msg) {
        String[] msgData = msg.split(":",2);
        if (msgData.length < 2) return;
        String system = msgData[0].trim().toLowerCase();

        sensor.SENSOR_SYSTEM sensSystem;
        switch (system) // determine sensors type, based on system. for SYSTEM_SENSORS type will be determined by jSON field.
        {
            case SYSTEM_SENSORS: sensSystem = sensor.SENSOR_SYSTEM.SENSES; break;
            case SYSTEM_CAME: sensSystem = sensor.SENSOR_SYSTEM.CAMES; break;
            case SYSTEM_SWITCHES: sensSystem = sensor.SENSOR_SYSTEM.SWITCHES; break;
            default: sensSystem = sensor.SENSOR_SYSTEM.UNKNOWN; break;
        }

        try {
            JSONObject jData = new JSONObject(msgData[1].trim());

            switch (jData.getString("type")) {
                case "cfg":
                    JSONObject jDataSens = new JSONObject(jData.getString("data"));
                    JSONArray sensors = jDataSens.getJSONArray(system);
                    for (int i = 0; i < sensors.length(); i++) {
                        JSONObject jSens = sensors.getJSONObject(i);

                        sensor sens = new sensor(jSens,sensSystem);
                        if (!replaceSens(sens.getAddr(),sens))
                            addSens(sens);
                    }
                    onSystemCfgChanged(sensSystem);
                    break;
                default:
                    if (jData.has("addr") && jData.has("data")) {
                        sensor sens = getSensByAddr(jData.getString("addr"));
                        if (sens != null) {
                            sens.setVal(jData.getDouble("data"));
                        }
                    }
                    break;
                }

        } catch (JSONException e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    private void onSystemCfgChanged(sensor.SENSOR_SYSTEM system) {
        if (mBoardType == BOARD_TYPE.REMOTE) return;    // do not request sensors data, web interface will do it for us
        List<sensor> sens = getSensors(system);
        for (int i = 0; i < sens.size(); i++) {
            sensor s = sens.get(i);
            updateSens(s);
        }
    }

    abstract public void updateSens(sensor sens);
    abstract public void onSensorAction(sensor sens, Object param);
    abstract public void sendPkt(byte[] data);
}
