package ru.wo0t.smarthouse.net;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
public class boardsDiscover {
    private final Handler mHandler;
    private UDPDiscover mUDPDiscover;

    public boardsDiscover(Context context, Handler handler)
    {
        mHandler = handler;
    }

    public void getBoardsList(int udpPort, String httpUser, String httpPasswd) {
        mUDPDiscover = new UDPDiscover(udpPort);
        mUDPDiscover.run();
    }

    public void close() {
        mUDPDiscover.close();
        mUDPDiscover = null;
    }


    private class UDPDiscover extends Thread {
        DatagramSocket mSocket = null;
        private final int mPort;
        public UDPDiscover(int port)
        {
            mPort = port;
            try {
                mSocket = new DatagramSocket(port);
            } catch (SocketException e) {
                e.printStackTrace();
                return;
            }
        }

        public String createDiscoverMsg() {
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
                InetAddress local = null;
                local = InetAddress.getByName("255.255.255.255");

                String msg = createDiscoverMsg();

                DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.length(),local, mPort);
                if (pkt == null)
                    Log.e("smhz","could not create datagram packet...");
                mSocket.send(pkt);
                Log.d("smhz", "Send descovery msg: "+msg + " to "+ local.toString() + ":" + mPort);
                final int msg_max_size = 1500;
                byte[] buf = new byte[msg_max_size];
                pkt = new DatagramPacket(buf, msg_max_size);

                while (!mSocket.isClosed()) {
                    mSocket.receive(pkt);
                    if (pkt.getLength() > 0) {
                        JSONObject jObjectData = new JSONObject(new String(buf,0,msg_max_size));
                        if (jObjectData.getString("id") != "smhz") continue;    // if non-board response - continue receiving...
                        jObjectData.put("ip_addr",pkt.getAddress());            // put board ip addr

                        mHandler.obtainMessage(constants.MESSAGE_NEW_BOARD,jObjectData).sendToTarget();
                    }

                }
            } catch (Exception e) {
                Log.e("smhz",e.toString());
                e.printStackTrace();
                return;
            }

        }
        public void close() {
            mSocket.close();
        }
    }
}
