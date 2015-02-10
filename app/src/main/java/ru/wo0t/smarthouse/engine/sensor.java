package ru.wo0t.smarthouse.engine;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */

public class sensor<T> {
    public static enum SENSOR_TYPE {UNKNOWN, TEMP, DIGITAL, SWITCH, CAME}
    private T mVal;
    private SENSOR_TYPE mSensType;
    private String mAddr;
    private String mName;

    public sensor() {}
    public sensor(JSONObject jObj){
        loadFromJSON(jObj);
    }
    public T getVal() { return mVal; }
    public void setVal(T val) { mVal = val; }

    public SENSOR_TYPE getType() { return mSensType; }
    public void setType(SENSOR_TYPE type) { mSensType = type; }

    public String getAddr() { return mAddr; }
    public void setAddr(String addr) { mAddr = addr; }

    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    public void loadFromJSON(JSONObject jObj) {
        try {
            if (jObj.has(constants.SENS_VAL))
                mVal = (T) jObj.get(constants.SENS_VAL);
            if (jObj.has(constants.SENS_TYPE)) {
                String sTypeStr = jObj.getString(constants.SENS_TYPE);
                SENSOR_TYPE sType = SENSOR_TYPE.UNKNOWN;
                switch (sTypeStr) {
                    case "T": sType = SENSOR_TYPE.TEMP; break;
                    case "D": sType = SENSOR_TYPE.DIGITAL; break;
                    case "S": sType = SENSOR_TYPE.SWITCH; break;
                    case "C": sType = SENSOR_TYPE.CAME; break;
                }
                mSensType = sType;
            }
            if (jObj.has(constants.SENS_ADDR))
                mAddr = jObj.getString(constants.SENS_ADDR);
            if (jObj.has(constants.SENS_NAME))
                mName = jObj.getString(constants.SENS_NAME);
        } catch (JSONException e) {
            Log.e(constants.APP_TAG,e.toString());
        }
    }

}
