package ru.wo0t.smarthouse.board;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */

public class sensor<T> {
    public static enum SENSOR_TYPE {UNKNOWN, TEMP, DIGITAL, SWITCH, CAME}
    public static enum SENSOR_SYSTEM {UNKNOWN, SENSES, SWITCHES, CAMES}
    private T mVal;
    private SENSOR_TYPE mSensType;
    private SENSOR_SYSTEM mSensSystem;
    private String mAddr;
    private String mName;
    private String mErrSign, mErrVal;
    private int mErrWarn;

    public sensor() {
        mSensType = SENSOR_TYPE.UNKNOWN;
    }
    public sensor(JSONObject jObj){
        loadFromJSON(jObj, SENSOR_SYSTEM.UNKNOWN);
    }
    public sensor(JSONObject jObj, SENSOR_SYSTEM sensSystem){
        loadFromJSON(jObj, sensSystem);
    }
    public T getVal() { return mVal; }
    public void setVal(T val) {
        mVal = val;
        Log.i(constants.APP_TAG, "Sensor " + mName +" new data: " + mVal);
    }

    public SENSOR_TYPE getType() { return mSensType; }
    public void setType(SENSOR_TYPE type) { mSensType = type; }

    public SENSOR_SYSTEM getSystem() { return mSensSystem; }
    public void setSystem(SENSOR_SYSTEM system) { mSensSystem = system; }

    public String getAddr() { return mAddr; }
    public void setAddr(String addr) { mAddr = addr; }

    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    public String getErrSign() { return mErrSign; }
    public void setErrSign(String sign) { mErrSign = sign; }
    public String getmErrVal() { return mErrVal; }
    public void setErrVal(String errVal) { mErrVal = errVal; }
    public int getErrWarn() { return mErrWarn; }
    public void setErrWarn(int errWarn) { mErrWarn = errWarn; }

    public void loadFromJSON(JSONObject jObj, SENSOR_SYSTEM sensSystem) {
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
            else {
                SENSOR_TYPE sType = SENSOR_TYPE.UNKNOWN;
                switch (sensSystem) // determine sensors type, based on system. for SYSTEM_SENSORS type will be determined by jSON field.
                {
                    case CAMES: sType = sensor.SENSOR_TYPE.CAME; break;
                    case SWITCHES: sType = sensor.SENSOR_TYPE.SWITCH; break;
                    default: sType = sensor.SENSOR_TYPE.UNKNOWN; break;
                }
                mSensType = sType;
            }

            mSensSystem = sensSystem;
            if (jObj.has(constants.SENS_ADDR))
                mAddr = jObj.getString(constants.SENS_ADDR);
            if (jObj.has(constants.SENS_NAME))
                mName = jObj.getString(constants.SENS_NAME);
            if (jObj.has(constants.SENS_ERR_SIGN))
                mErrSign = jObj.getString(constants.SENS_ERR_SIGN);
            if (jObj.has(constants.SENS_ERR_VAL))
                mErrVal = jObj.getString(constants.SENS_ERR_VAL);
            if (jObj.has(constants.SENS_ERR_WARN))
                mErrWarn = jObj.getInt(constants.SENS_ERR_WARN);
        } catch (JSONException e) {
            Log.e(constants.APP_TAG,e.toString());
        }
    }

}
