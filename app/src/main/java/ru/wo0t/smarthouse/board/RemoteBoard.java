package ru.wo0t.smarthouse.board;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */
public class RemoteBoard extends AbstractBoard {
    private httpClient mClient;

    public RemoteBoard(BOARD_TYPE type, int id, String name, String login, String password) {
        super(type, id);
        mClient = new httpClient(mHandler, login, password, id);
        mClient.start();
    }

    public void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        httpClient cl;
        synchronized (this)
        {
            cl = mClient;
        }
        cl.sendPkt(pkt);
    }

    public void updateSens(Sensor sens) {
        String cmd = "";
        switch (sens.getSystem()){
            case SWITCHES:
                cmd = SYSTEM_SWITCHES + " get " + "p" + sens.getAddr();
                break;
            case SENSES:
                String tpStr = "";
                switch (sens.getType()) {
                    case TEMP:
                        tpStr = "T";
                        break;
                    case DIGITAL:
                        tpStr = "D";
                        break;
                }
                cmd = SYSTEM_SENSORS + " get " + tpStr+ sens.getAddr();
                break;
            case CAMES:
                cmd = SYSTEM_CAME + " get " + "c" + sens.getAddr();
                break;
        }
        sendPkt(cmd.getBytes());
    }

    public void onSensorAction(Sensor sens, Object param) {
        String cmd = "";
        switch (sens.getSystem()){
            case SWITCHES:
                cmd = SYSTEM_SWITCHES + " set " + "p" + sens.getAddr() + "=" + param ;
                break;
            case SENSES:
                String tpStr = "";
                switch (sens.getType()) {
                    case TEMP:
                        tpStr = "T";
                        break;
                    case DIGITAL:
                        tpStr = "D";
                        break;
                }
                cmd = SYSTEM_SENSORS + " get " + tpStr+ sens.getAddr();
                break;
            case CAMES:
                cmd = SYSTEM_CAME + " get " + "c" + sens.getAddr();
                break;
        }
        sendPkt(cmd.getBytes());

    }
////////////////////////////////////////////////////////////////////////////////

    private class outMsg {
        public byte[] data;
        public String cmd;

    }
    private class httpClient extends Thread {
        String mLogin, mPassword;
        Handler mHandler;
        String mUrlString = constants.REMOTE_BOARD_URL_STRING;
        int mBoardId;
        int mUpdTime;
        int mSockReadTimeout; // seconds
        Queue<outMsg> mOutQueue;

        httpClient(Handler handler, String login, String password, int board_id) {
            mHandler = handler;
            mLogin = login;
            mPassword = password;
            mBoardId = board_id;
            mUpdTime = 0;
            mSockReadTimeout = 10;
            mOutQueue = new LinkedList<outMsg>();
        }

        private String loadFromNetwork(String cmd, String message) throws IOException {
            InputStream stream = null;
            String str = "";

            try {
                stream = downloadUrl(cmd, message);
                str = readIt(stream, 2048);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
            return str;
        }

        private InputStream downloadUrl(String cmd, String message) throws IOException {

            URL url = new URL(mUrlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(mSockReadTimeout * 1000 + 3000/* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // set basic auth
            String userPassword = constants.REMOTE_BOARD_USER + ":" + constants.REMOTE_BOARD_PASSWORD;
            String encoding = Base64.encodeToString(userPassword.getBytes(),Base64.DEFAULT);
            conn.setRequestProperty("Authorization", "Basic " + encoding);

            //send POST params
            Hashtable<String, String> params = new Hashtable<String, String>();
            params.put("board_id", String.valueOf(mBoardId));
            params.put("cmd", cmd);
            params.put("msg", message);
            params.put("time", Integer.toString(mUpdTime));
            params.put("login", mLogin);
            params.put("password", mPassword);
            String postParamsStr = getPostParamString(params);
            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "Content-Length", Integer.toString( postParamsStr.length() ));
            conn.getOutputStream().write(postParamsStr.getBytes("UTF-8"));

            InputStream stream = conn.getInputStream();

            return stream;
        }

        private String getPostParamString(Hashtable<String, String> params) {
            if(params.size() == 0)
                return "";
            StringBuffer buf = new StringBuffer();
            Enumeration<String> keys = params.keys();
            while(keys.hasMoreElements()) {
                buf.append(buf.length() == 0 ? "" : "&");
                String key = keys.nextElement();
                buf.append(key).append("=").append(params.get(key));
            }
            return buf.toString();
        }

        private String readIt(InputStream stream, int len) throws IOException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];

            int readBytes = 0;
            String ret = "";
            while ( (readBytes = reader.read(buffer)) > 0) {
                String s = new String(buffer,0,readBytes);
                ret += s;
            }

            return ret;
        }


        @Override
        public void run()
        {
            while (!this.isInterrupted() && mUpdTime == 0) {
                write("register", new String("").getBytes());
            }

            while (!this.isInterrupted())
            {
                if (mOutQueue.size() > 0) {
                    while (mOutQueue.size() > 0) {
                        outMsg msg = mOutQueue.poll();
                        write(msg.cmd, msg.data);
                    }
                }
                else
                    write("ping", new String("").getBytes());

                try {
                    Thread.sleep(constants.REMOTE_BOARD_WAIT_PERIOD);

                } catch (InterruptedException e) {
                    Log.e(constants.APP_TAG, e.toString());
                }
            }
        }

        public void sendPkt(String cmd, byte[] buf)
        {
            outMsg msg = new outMsg();
            msg.cmd = cmd;
            msg.data = buf;
            mOutQueue.add(msg);
        }

        public void sendPkt(byte[] buf) {
            sendPkt("message", buf);
        }

        public void write(String cmd, byte[] buf) {
            try {
                String recvData = loadFromNetwork(cmd, new String(buf));
                Log.i(constants.APP_TAG, "Sending " + cmd + " " + new String(buf));
                if (recvData.length() > 0)
                    onMsgRecv(recvData);
            } catch (Exception e) {
                Log.e(constants.APP_TAG, e.toString());
            }
        }

        private void onMsgRecv(String msgData)
        {
            Log.i(constants.APP_TAG, "Recv: " + msgData);
            String board_data = "";

            try {
                JSONObject jObj = new JSONObject(msgData);
                board_data = jObj.getString("board_data");

                if (jObj.has("time"))
                    mUpdTime = jObj.getInt("time");
                if (jObj.has("cmd"))
                    switch (jObj.getString("cmd"))
                    {
                        case "register":
                            if (jObj.getString("answer").equals("OK") )
                            {
                                mSockReadTimeout = jObj.getInt("timeout");
                                Message msg = mHandler.obtainMessage(constants.MESSAGE_CONNECTED);
                                Bundle bundle = new Bundle();
                                bundle.putString(constants.MESSAGE_INFO, "Successfully connected to remote board");
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }
                        break;
                    }
            } catch (JSONException e) {
                Log.e(constants.APP_TAG, e.toString());
                return;
            }

            if (board_data.length() > 0) {
                try {
                    Object jObj = new JSONArray(board_data);

                    if (jObj instanceof JSONArray == false) //
                    {
                        Message msg = mHandler.obtainMessage(constants.MESSAGE_NEW_MSG);
                        Bundle bundle = new Bundle();
                        bundle.putString(constants.MESSAGE_INFO, "Recv data from " + mUrlString);
                        bundle.putByteArray(constants.MESSAGE_DATA, board_data.getBytes());
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                    else
                    {
                        JSONArray boardArrData = (JSONArray)jObj;
                        for (int i = 0; i < boardArrData.length(); i++) {
                            String dat = boardArrData.getString(i);

                            Message msg = mHandler.obtainMessage(constants.MESSAGE_NEW_MSG);
                            Bundle bundle = new Bundle();
                            bundle.putString(constants.MESSAGE_INFO, "Recv data from " + mUrlString);
                            bundle.putByteArray(constants.MESSAGE_DATA, dat.getBytes());
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    Log.e(constants.APP_TAG, e.toString());
                }
            }
        }
    }
}
