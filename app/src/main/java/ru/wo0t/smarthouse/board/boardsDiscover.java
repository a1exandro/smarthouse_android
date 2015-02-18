package ru.wo0t.smarthouse.board;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.common.constants;

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
            Log.e("smhz", e.toString());
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
                Log.e("smhz", e.toString());
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
                Log.e("smhz", e.toString());
                return "";
            }
        }

        public void run() {
            try {
                InetAddress local = null;
                local = InetAddress.getByName("192.168.222.134");

                String msg = createDiscoverMsg();

                DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.length(),local, mPort);

                mSocket.send(pkt);
                Log.d("smhz", "Send discovery msg: "+msg + " to "+ local.getHostAddress() + ":" + mPort);
                final int msg_max_size = 1500;
                byte[] buf = new byte[msg_max_size];
                pkt = new DatagramPacket(buf, msg_max_size);

                while (!mSocket.isClosed()) {
                    mSocket.receive(pkt);
                    if (pkt.getLength() > 0) {
                        String reply = new String(buf,0,pkt.getLength());
                        JSONObject jObjectData = new JSONObject(reply);
                        if (!jObjectData.getString("id").equals(constants.BOARD_KEYWORD)) continue;    // if non-board response - continue receiving...
                        jObjectData.put("ip_addr",pkt.getAddress().getHostAddress());                 // put board ip addr
                        jObjectData.put("board_type", AbstractBoard.BOARD_TYPE.LOCAL);                  // put board type LOCAL

                        mHandler.obtainMessage(constants.MESSAGE_NEW_BOARD,jObjectData).sendToTarget();
                    }

                }
            } catch (Exception e) {
                Log.e("smhz", e.toString());
                return;
            }

        }
        public void close() {
            mSocket.close();
        }
    }

    private class RemoteDiscover extends Thread {
        String mHttpUser, mHttpPasswd;
        public RemoteDiscover(String httpUser, String httpPasswd)
        {
            mHttpUser = httpUser;
            mHttpPasswd = httpPasswd;

        }

        public void run() {
            try {
                JSONObject jObjectData = new JSONObject();
                jObjectData.put("board_id", 1);
                jObjectData.put("login", mHttpUser);
                jObjectData.put("password", mHttpPasswd);
                jObjectData.put("board_type", AbstractBoard.BOARD_TYPE.REMOTE);

                mHandler.obtainMessage(constants.MESSAGE_NEW_BOARD,jObjectData).sendToTarget();
            } catch (Exception e) {
                Log.e("smhz", e.toString());
            }

        }
        public void close() {

        }
    }
}
