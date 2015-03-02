package ru.wo0t.smarthouse.board;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import ru.wo0t.smarthouse.common.constants;
import ru.wo0t.smarthouse.common.funcs;

/**
 * Created by alex on 2/5/15.
 */
public class boardsDiscover extends AsyncTask<Object, Void, Boolean> {
    public final static int LOOKUP_LOCAL_BOARD = 0x01;
    public final static int LOOKUP_REMOTE_BOARD = 0x01 << 1;
    public final static int LOOKUP_ALL_BOARDS = LOOKUP_LOCAL_BOARD | LOOKUP_REMOTE_BOARD;

    private final Handler mHandler;
    private int mLookUpFlag;
    private LocalDiscover mLocalDiscover;
    private RemoteDiscover mRemoteDiscover;


    @Override
    protected Boolean doInBackground(Object... params) {

        int udpPort = (int)params[0];
        String httpUser = (String)params[1];
        String httpPasswd = (String)params[2];
        getBoardsList(udpPort, httpUser, httpPasswd, mLookUpFlag);
        mHandler.obtainMessage(constants.MESSAGE_DISCOVERY_FINISHED).sendToTarget();
        close();
        return true;
    }


    public boardsDiscover(Handler handler, int lookup_flags)
    {
        mHandler = handler;
        mLookUpFlag = lookup_flags;
    }

    private void getBoardsList(int udpPort, String httpUser, String httpPasswd, int lookup_flags) {
        if ((lookup_flags & LOOKUP_LOCAL_BOARD) > 0) {
            mLocalDiscover = new LocalDiscover(udpPort);
            mLocalDiscover.start();
        }
        if ((lookup_flags & LOOKUP_REMOTE_BOARD) > 0) {
            mRemoteDiscover = new RemoteDiscover(httpUser, httpPasswd);
            mRemoteDiscover.start();
        }

        try {
            for (int i = 0; i < 100; i++) {
                Thread.sleep(constants.BOARD_LOOKUP_TIMEOUT/100);
            }
        } catch (InterruptedException e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    public void close() {
        if (mLocalDiscover != null) {
            mLocalDiscover.close();
            mLocalDiscover = null;
        }
        if (mLocalDiscover != null) {
            mLocalDiscover.close();
            mLocalDiscover = null;
        }
    }


    private class LocalDiscover extends Thread {
        DatagramSocket mSocket = null;
        private final int mPort;
        public LocalDiscover(int port)
        {
            mPort = port;
            try {
                mSocket = new DatagramSocket(port);
            } catch (SocketException e) {
                Log.e(constants.APP_TAG, e.toString());
                return;
            }
        }

        private String createDiscoverMsg() {
            try
            {
                JSONObject jObjectData = new JSONObject();

                jObjectData.put("id", "smhz");
                jObjectData.put("vers", constants.version);

                return jObjectData.toString();
            }
            catch(Exception e)
            {
                Log.e(constants.APP_TAG, e.toString());
                return "";
            }
        }

        public void run() {
            try {
                InetAddress local = null;
                local = InetAddress.getByName("192.168.222.136");

                String msg = createDiscoverMsg();

                DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.length(),local, mPort);

                mSocket.send(pkt);
                Log.d(constants.APP_TAG, "Send discovery msg: "+msg + " to "+ local.getHostAddress() + ":" + mPort);
                final int msg_max_size = 1500;
                byte[] buf = new byte[msg_max_size];
                pkt = new DatagramPacket(buf, msg_max_size);

                while (!mSocket.isClosed()) {
                    mSocket.receive(pkt);
                    if (pkt.getLength() > 0) {
                        String reply = new String(buf,0,pkt.getLength());
                        JSONObject jObjectData = new JSONObject(reply);
                        if (!jObjectData.getString("id").equals(constants.BOARD_KEYWORD)) continue;    // if non-board response - continue receiving...

                        jObjectData.put("descr",pkt.getAddress().getHostAddress());                 // put board ip addr
                        jObjectData.put("ip_addr",pkt.getAddress().getHostAddress());                 // put board ip addr
                        jObjectData.put("board_type", AbstractBoard.BOARD_TYPE.LOCAL);                  // put board type LOCAL

                        mHandler.obtainMessage(constants.MESSAGE_NEW_BOARD,jObjectData).sendToTarget();
                    }

                }
            } catch (Exception e) {
                Log.e(constants.APP_TAG, e.toString());
                return;
            }

        }
        public void close() {
            mSocket.close();
        }
    }

    private class RemoteDiscover extends Thread {
        String mLogin, mPassword;
        int mSockReadTimeout = 10;
        String mUrlString = constants.REMOTE_BOARD_URL_STRING;

        public RemoteDiscover(String httpUser, String httpPasswd)
        {
            mLogin = httpUser;
            mPassword = httpPasswd;
        }

        public void run() {
            try {
                String reply = loadFromNetwork("board_list_req","");
                JSONObject jReply = new JSONObject(reply);
                String salt = "";
                if (jReply.has("salt")) salt = jReply.getString("salt");
                String password = funcs.md5(mPassword + salt);

                JSONArray boards = jReply.getJSONArray("boards");
                for (int i = 0; i < boards.length(); i++) {
                    JSONObject jBoard = boards.getJSONObject(i);

                    jBoard.put("login", mLogin);
                    jBoard.put("password", password);
                    jBoard.put("board_type", AbstractBoard.BOARD_TYPE.REMOTE);
                    jBoard.put("board_id",jBoard.getInt("id"));
                    mHandler.obtainMessage(constants.MESSAGE_NEW_BOARD,jBoard).sendToTarget();
                }
            } catch (Exception e) {
                Log.e(constants.APP_TAG, e.toString());
            }

        }
        public void close() {

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
            String encoding = Base64.encodeToString(userPassword.getBytes(), Base64.DEFAULT);
            conn.setRequestProperty("Authorization", "Basic " + encoding);

            //send POST params
            Hashtable<String, String> params = new Hashtable<String, String>();

            params.put("cmd", cmd);
            params.put("msg", message);

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
    }
}
