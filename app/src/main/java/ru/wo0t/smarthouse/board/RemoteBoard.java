package ru.wo0t.smarthouse.board;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
    private String mLogin, mPassword;
    public RemoteBoard(Context context, BOARD_TYPE type, int id, String name, String login, String password) {
        super(context, type, id, name);
        mLogin = login;
        mPassword = password;
        mClient = new httpClient(login, password, id);
        mClient.start();
    }

    public String getLogin() { return mLogin; }
    public String getPassword() { return mPassword; }

    public void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        httpClient cl;
        synchronized (this)
        {
            cl = mClient;
        }
        cl.sendPkt(pkt);
    }

    @Override
    public void close() {
        super.close();
        mClient.interrupt();
        mClient = null;
    }
////////////////////////////////////////////////////////////////////////////////

    private class outMsg {
        public byte[] data;
        public String cmd;

    }
    private class httpClient extends Thread {
        String mLogin, mPassword;
        int mBoardId;
        int mLastMsgId;
        int mSockReadTimeout; // seconds
        Queue<outMsg> mOutQueue;
        HttpURLConnection mConn;

        httpClient(String login, String password, int board_id) {
            mLogin = login;
            mPassword = password;
            mBoardId = board_id;
            mLastMsgId = 0;
            mSockReadTimeout = 10;
            mOutQueue = new LinkedList<>();
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
            return downloadUrl(constants.REMOTE_BOARD_URL_HOST + constants.REMOTE_BOARD_URL_STRING, cmd, message);
        }

        private InputStream downloadUrl(String urlString, String cmd, String message) throws IOException {

            URL url = new URL(urlString);
            mConn = (HttpURLConnection) url.openConnection();
            mConn.setReadTimeout(mSockReadTimeout * 1000 + 3000/* milliseconds */);
            mConn.setConnectTimeout(15000 /* milliseconds */);
            mConn.setRequestMethod("POST");
            mConn.setDoInput(true);
            mConn.setDoOutput(true);

            // set basic auth
            String userPassword = constants.REMOTE_BOARD_USER + ":" + constants.REMOTE_BOARD_PASSWORD;
            String encoding = Base64.encodeToString(userPassword.getBytes(),Base64.DEFAULT);
            mConn.setRequestProperty("Authorization", "Basic " + encoding);

            //send POST params
            Hashtable<String, String> params = new Hashtable<>();
            params.put("board_id", String.valueOf(mBoardId));
            params.put("cmd", cmd);
            params.put("msg", message);
            params.put("lastMsgId", Integer.toString(mLastMsgId));
            params.put("login", mLogin);
            params.put("password", mPassword);
            String postParamsStr = getPostParamString(params);
            mConn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
            mConn.setRequestProperty( "Content-Length", Integer.toString( postParamsStr.length() ));
            mConn.getOutputStream().write(postParamsStr.getBytes("UTF-8"));

            return mConn.getInputStream();
        }

        private String getPostParamString(Hashtable<String, String> params) {
            if(params.size() == 0)
                return "";
            StringBuilder buf = new StringBuilder();
            Enumeration<String> keys = params.keys();
            while(keys.hasMoreElements()) {
                buf.append(buf.length() == 0 ? "" : "&");
                String key = keys.nextElement();
                buf.append(key).append("=").append(params.get(key));
            }
            return buf.toString();
        }

        private String readIt(InputStream stream, int len) throws IOException {
            Reader reader;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];

            int readBytes ;
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
            while (!this.isInterrupted() && mLastMsgId == 0) {
                write("register", new String().getBytes());
                try {
                    if (mLastMsgId == 0)
                        Thread.sleep(constants.REMOTE_BOARD_WAIT_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (!this.isInterrupted())
            {
                boolean sendOk = false;
                if (mOutQueue.size() > 0) {
                    while (mOutQueue.size() > 0) {
                        outMsg msg = mOutQueue.poll();
                        sendOk = write(msg.cmd, msg.data);
                    }
                }
                else
                    sendOk = write("ping", new String().getBytes());
                if (!sendOk && mOutQueue.size() == 0) {
                    try {
                        Thread.sleep(constants.REMOTE_BOARD_WAIT_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            mConn.disconnect();
        }

        public void sendPkt(String cmd, byte[] buf)
        {
            outMsg msg = new outMsg();
            msg.cmd = cmd;
            msg.data = buf;
            mOutQueue.add(msg);
            mConn.disconnect();
        }

        public void sendPkt(byte[] buf) {
            sendPkt("message", buf);
        }

        public boolean write(String cmd, byte[] buf) {
            try {
                Log.i(constants.APP_TAG, "Sending " + cmd + " " + new String(buf));
                String recvData = loadFromNetwork(cmd, new String(buf));
                if (recvData.length() > 0)
                    return onMsgRecv(recvData);
                else
                    return false;
            } catch (Exception e) {
                //e.printStackTrace();
                Log.d(constants.APP_TAG, e.toString());
                return false;
            }
        }

        private Object getExtraData(String msg) {
            Object eData = null;
            String[] msgData = msg.split(":",2);
            if (msgData.length < 2) return null;
            int offset = 0;
            try {
                JSONObject jObj = new JSONObject(msgData[1].trim());
                if (jObj.has("type")) {
                    switch (jObj.getString("type")) {
                        case "picture": // get camera image
                            if (jObj.has("fname")) {
                                JSONArray jFiles = new JSONArray(jObj.getString("fname"));
                                String fName = new File(jFiles.get(0).toString()).getName();
                                String fullUrl = constants.REMOTE_BOARD_URL_HOST + "modules/camera/img/" + mBoardId + "/" + fName;
                                InputStream input = downloadUrl(fullUrl," "," ");


                                byte[] picData = new byte[mConn.getContentLength()];

                                while ( (offset += input.read(picData,offset, mConn.getContentLength() - offset)) > 0);
                                eData = picData;
                            }
                            break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return eData;
        }

        private boolean onMsgRecv(String msgData)
        {
            Log.i(constants.APP_TAG, "Recv: " + msgData);
            String board_data;

            try {
                JSONObject jObj = new JSONObject(msgData);
                board_data = jObj.getString("board_data");

                if (jObj.has("lastMsgId"))
                    mLastMsgId = jObj.getInt("lastMsgId");
                if (jObj.has("cmd")) {
                    switch (jObj.getString("cmd")) {
                        case "register":
                            if (jObj.getString("answer").equals("OK")) {
                                mSockReadTimeout = jObj.getInt("timeout");
                                onBoardConnected();
                            }
                            break;
                    }
                }
                else
                    return false;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }

            if (board_data.length() > 0) {
                try {
                    Object jObj = new JSONArray(board_data);

                    if (!(jObj instanceof JSONArray)) //
                    {
                        messageParser(board_data, getExtraData(board_data));
                    }
                    else
                    {
                        JSONArray boardArrData = (JSONArray)jObj;
                        for (int i = 0; i < boardArrData.length(); i++) {
                            String dat = boardArrData.getString(i);
                            messageParser(dat, getExtraData(dat));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }
    }
}
