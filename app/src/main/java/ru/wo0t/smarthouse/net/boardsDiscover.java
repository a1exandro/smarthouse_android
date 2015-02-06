package ru.wo0t.smarthouse.net;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/5/15.
 */
public class boardsDiscover extends AsyncTask<Object, Void, Void> {
    private final Handler mHandler;
    private LocalDiscover mLocalDiscover;
    private RemoteDiscover mRemoteDiscover;
    @Override
    protected Void doInBackground(Object... params) {

        int udpPort = (int)params[0];
        String httpUser = (String)params[1];
        String httpPasswd = (String)params[2];
        getBoardsList(udpPort, httpUser, httpPasswd);
        return null;
    }


    public boardsDiscover(Context context, Handler handler)
    {
        mHandler = handler;
    }

    private void getBoardsList(int udpPort, String httpUser, String httpPasswd) {
        mLocalDiscover = new LocalDiscover(udpPort);
        mLocalDiscover.start();

        mRemoteDiscover = new RemoteDiscover(httpUser, httpPasswd);
        mRemoteDiscover.start();

        try {
            Thread.sleep(5000);
            mHandler.obtainMessage(constants.MESSAGE_DISCOVERY_FINISHED).sendToTarget();
            close();
        } catch (InterruptedException e) {
            Log.e("smhz", e.toString());
        }
    }

    public void close() {
        mLocalDiscover.close();
        mLocalDiscover = null;

        mRemoteDiscover.close();
        mRemoteDiscover = null;
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
                Log.d("smhz", "Send discovery msg: "+msg + " to "+ local.toString() + ":" + mPort);
                final int msg_max_size = 1500;
                byte[] buf = new byte[msg_max_size];
                pkt = new DatagramPacket(buf, msg_max_size);

                while (!mSocket.isClosed()) {
                    mSocket.receive(pkt);
                    if (pkt.getLength() > 0) {
                        String reply = new String(buf,0,pkt.getLength());
                        JSONObject jObjectData = new JSONObject(reply);
                        if (!jObjectData.getString("id").equals(constants.BOARD_KEYWORD)) continue;    // if non-board response - continue receiving...
                        jObjectData.put("ip_addr",pkt.getAddress());                // put board ip addr
                        jObjectData.put("board_type",constants.BOARD_TYPE.LOCAL);   // put board type LOCAL

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

            } catch (Exception e) {
                Log.e("smhz", e.toString());
                return;
            }

        }
        public void close() {

        }
    }
}
