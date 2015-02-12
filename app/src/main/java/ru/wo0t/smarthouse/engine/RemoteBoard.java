package ru.wo0t.smarthouse.engine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */
public class RemoteBoard extends board {
    private httpClient mClient;

    public RemoteBoard(BOARD_TYPE type, int id, String name, String login, String password) {
        super(type, id);
        mClient = new httpClient(mHandler, login, password, id);
        mClient.execute();
    }

    private void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        httpClient cl;
        synchronized (this)
        {
            cl = mClient;
        }
        cl.sendPkt(pkt);
    }

    @Override
    protected void messageParser(String msg) {
        super.messageParser(msg);
    }

    private final Handler mHandler = new Handler() {
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

    public void updateSens(sensor sens) {
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

    public void onSensorAction(sensor sens, Object param) {
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

    private class httpClient extends AsyncTask<Object, Void, Void> {
        String mLogin, mPassword;
        Handler mHandler;
        String mUrlString = constants.REMOTE_BOARD_URL_STRING;
        int mBoardId;

        httpClient(Handler handler, String login, String password, int board_id) {
            mHandler = handler;
            mLogin = login;
            mPassword = password;
            mBoardId = board_id;
        }

        private String loadFromNetwork(String cmd, String message) throws IOException {
            InputStream stream = null;
            String str = "";

            try {
                stream = downloadUrl(cmd, message);
                str = readIt(stream, 1024);
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
            conn.setReadTimeout(10000 /* milliseconds */);
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


            String postParamsStr = getPostParamString(params);
            Log.i("smhz","params: "+postParamsStr);
            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty( "Content-Length", Integer.toString( postParamsStr.length() ));
            conn.setUseCaches( false );

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(postParamsStr);

            conn.connect();
            // Start the query

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
        private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            int readBytes = reader.read(buffer);
            String ret = "";
            if (readBytes > 0)
                ret = new String(buffer,0,readBytes);
            return ret;
        }


        @Override
        protected Void doInBackground(Object... params)
        {
            while (!this.isCancelled())
            {
                try {
                    String req = loadFromNetwork("ping","");
                    if (req.length() > 0) {
                        onMsgRecv(req);
                    }
                } catch (IOException e) {
                    Log.e(constants.APP_TAG, e.toString());
                }

                try {
                    Thread.sleep(constants.REMOTE_BOARD_WAIT_PERIOD);

                } catch (InterruptedException e) {
                    Log.e(constants.APP_TAG, e.toString());
                }
            }
            return null;
        }

        public void sendPkt(byte[] buf) {
            try {
                if (buf.length == 0) return;
                String recvData = loadFromNetwork("request", new String(buf));
                if (recvData.length() > 0)
                    onMsgRecv(recvData);
            } catch (Exception e) {
                Log.e(constants.APP_TAG, e.toString());
            }
        }
        private void onMsgRecv(String msgData)
        {
            Message msg = mHandler.obtainMessage(constants.MESSAGE_NEW_MSG);
            Bundle bundle = new Bundle();
            bundle.putString(constants.MESSAGE_INFO, "Recv data from "+mUrlString);
            bundle.putByteArray(constants.MESSAGE_DATA, msgData.getBytes());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }
}
