package ru.wo0t.smarthouse.board;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */

public class Sensor<T> {
    public static enum SENSOR_TYPE {UNKNOWN, TEMP, DIGITAL, SWITCH, CAME}
    public static enum SENSOR_SYSTEM {UNKNOWN, SENSES, SWITCHES, CAMES}
    private T mVal;
    private SENSOR_TYPE mSensType;
    private SENSOR_SYSTEM mSensSystem;
    private String mAddr;
    private String mName;
    private String mErrSign, mErrVal;
    private int mErrWarn;
    private boolean mIsNotified = false;

    public Sensor() {
        mSensType = SENSOR_TYPE.UNKNOWN;
    }
    public Sensor(JSONObject jObj){
        loadFromJSON(jObj, SENSOR_SYSTEM.UNKNOWN);
    }
    public Sensor(JSONObject jObj, SENSOR_SYSTEM sensSystem){
        loadFromJSON(jObj, sensSystem);
    }
    public T getVal() { return mVal; }
    public void setVal(T val) {
        mVal = val;
        if (mIsNotified) {
            if (checkValue()) mIsNotified = false;
        }
        //Log.i(constants.APP_TAG, "Sensor " + mName +" new data: " + mVal);
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
    public String getErrVal() { return mErrVal; }
    public void setErrVal(String errVal) { mErrVal = errVal; }
    public int getErrWarn() { return mErrWarn; }
    public void setErrWarn(int errWarn) { mErrWarn = errWarn; }

    public String getStringValue(Context context) {
        String sensVal = "";
        switch (mSensType) {
            case TEMP:
                sensVal = String.valueOf((double) (Object)mVal) +"°C";
                break;
            case DIGITAL:
                if (((double) (Object)mVal) == Double.valueOf(mErrVal))
                    sensVal = context.getString(R.string.not_ok);
                else
                    sensVal = context.getString(R.string.OK);
                break;
        }
        return sensVal;
    }
    /* returns true if sensor value is ok, else returns false */
    public boolean isNotified() {
        if (mErrWarn == 0) return true;
        return mIsNotified;
    }
    public void setNotified(boolean notified) { mIsNotified = notified; }
    public boolean checkValue(){
        if (mErrVal == null || mErrSign == null || mVal == null) return true;

        switch (mSensSystem) {
            case SENSES: {
                switch (mErrSign) {
                    case "0": { // less
                        if ((double)(Object)getVal() < Double.valueOf(mErrVal)) {
                            return false;
                        }
                        else return true;
                    }
                    case "1": { // equal
                        if ((double)(Object)getVal() == Double.valueOf(mErrVal)) {
                            return false;
                        }
                        else return true;
                    }
                    case "2": { // more
                        if ((double)(Object)getVal() > Double.valueOf(mErrVal)) {
                            return false;
                        }
                        else return true;
                    }
                }
            }break;
        }

        return true;
    }

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
                SENSOR_TYPE sType;
                switch (sensSystem) // determine sensors type, based on system. for SYSTEM_SENSORS type will be determined by jSON field.
                {
                    case CAMES: sType = Sensor.SENSOR_TYPE.CAME; break;
                    case SWITCHES: sType = Sensor.SENSOR_TYPE.SWITCH; break;
                    default: sType = Sensor.SENSOR_TYPE.UNKNOWN; break;
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
