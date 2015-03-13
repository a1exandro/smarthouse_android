package ru.wo0t.smarthouse.board;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

    private final Context mContext;
    private int mLookUpFlag;
    private LocalDiscover mLocalDiscover;
    private RemoteDiscover mRemoteDiscover;

    private void sendBroadcastMsg(String event, Bundle args) {
        Intent intent = new Intent(event);
        intent.putExtras(args);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    @Override
    protected Boolean doInBackground(Object... params) {

        int udpPort = (int)params[0];
        String httpUser = (String)params[1];
        String httpPasswd = (String)params[2];
        getBoardsList(udpPort, httpUser, httpPasswd, mLookUpFlag);
        while (isCancelled()) {
            try {
                Thread.sleep(constants.BOARD_LOOKUP_TIMEOUT/100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sendBroadcastMsg(boardsManager.MSG_BOARDS_DISCOVERY_FINISHED, new Bundle());
        close();
        return true;
    }


    public boardsDiscover(Context context, int lookup_flags)
    {
        mContext = context;
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
    }

    public void close() {
        if (mLocalDiscover != null) {
            mLocalDiscover.close();
            mLocalDiscover = null;
        }
        if (mRemoteDiscover != null) {
            mRemoteDiscover.close();
            mRemoteDiscover = null;
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
                e.printStackTrace();
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
                e.printStackTrace();
                return "";
            }
        }

        public void run() {
            try {
                InetAddress local;
                mSocket.setBroadcast(true);
                local = InetAddress.getByName("255.255.255.255");

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
                        if (!jObjectData.getString("id").equals(constants.LOCAL_BOARD_KEYWORD)) continue;    // if non-board response - continue receiving...
                        if (!jObjectData.has("board_id")) continue; // out request without board_id, skip it
                        Bundle args = new Bundle();
                        args.putInt(boardsManager.BOARD_ID, jObjectData.getInt("board_id"));
                        args.putString(boardsManager.BOARD_DESCR, pkt.getAddress().getHostAddress());
                        args.putString(boardsManager.BOARD_IP_ADDR, pkt.getAddress().getHostAddress());
                        args.putString(boardsManager.BOARD_TYPE, AbstractBoard.BOARD_TYPE.LOCAL.toString());
                        sendBroadcastMsg(boardsManager.MSG_FOUND_NEW_BOARD, args);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        public void close() {
            if (mSocket != null)
                mSocket.close();
        }
    }

    private class RemoteDiscover extends Thread {
        String mLogin, mPassword;
        int mSockReadTimeout = 10;
        String mUrlString = constants.REMOTE_BOARD_URL_STRING;
        HttpURLConnection mConn;

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

                    Bundle args = new Bundle();
                    args.putInt(boardsManager.BOARD_ID, jBoard.getInt("id"));
                    args.putString(boardsManager.BOARD_DESCR, jBoard.getString("descr"));
                    args.putString(boardsManager.BOARD_LOGIN, mLogin);
                    args.putString(boardsManager.BOARD_PW, password);
                    args.putString(boardsManager.BOARD_TYPE, AbstractBoard.BOARD_TYPE.REMOTE.toString());

                    sendBroadcastMsg(boardsManager.MSG_FOUND_NEW_BOARD, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        public void close() {
            if (mConn != null)
                mConn.disconnect();
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
            mConn = (HttpURLConnection) url.openConnection();
            mConn.setReadTimeout(mSockReadTimeout * 1000 + 3000/* milliseconds */);
            mConn.setConnectTimeout(15000 /* milliseconds */);
            mConn.setRequestMethod("POST");
            mConn.setDoInput(true);
            mConn.setDoOutput(true);

            // set basic auth
            String userPassword = constants.REMOTE_BOARD_USER + ":" + constants.REMOTE_BOARD_PASSWORD;
            String encoding = Base64.encodeToString(userPassword.getBytes(), Base64.DEFAULT);
            mConn.setRequestProperty("Authorization", "Basic " + encoding);

            //send POST params
            Hashtable<String, String> params = new Hashtable<>();

            params.put("cmd", cmd);
            params.put("msg", message);

            params.put("login", mLogin);
            params.put("password", mPassword);
            String postParamsStr = getPostParamString(params);
            mConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            mConn.setRequestProperty("Content-Length", Integer.toString(postParamsStr.length()));
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

            int readBytes;
            String ret = "";
            while ( (readBytes = reader.read(buffer)) > 0) {
                String s = new String(buffer,0,readBytes);
                ret += s;
            }

            return ret;
        }
    }
}
