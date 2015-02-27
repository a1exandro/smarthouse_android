package ru.wo0t.smarthouse.board;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */
public class LocalBoard extends AbstractBoard {
    private String mIpAddr;
    private tcpClient mClient;

    public LocalBoard(Context context, BOARD_TYPE type, int id, String name, String ipAddr) {
        super(context, type, id);
        mIpAddr = ipAddr;
        mClient = new tcpClient(mHandler,ipAddr,constants.LOCAL_BOARD_PORT);
        mClient.start();
    }

    public void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        tcpClient cl;
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

    private class tcpClient extends Thread {
        Socket mSock;
        String mHost;
        int mPort;
        Handler mHandler;
        Queue<byte[]> mOutQueue;

        tcpClient(Handler handler,String host, int port) {
            mHandler = handler;
            mHost = host;
            mPort = port;
            mOutQueue = new LinkedList<>();
        }

        private boolean connectToHost(String host, int port)
        {
            try {
                Log.i(constants.APP_TAG, "Connecting to " + host +":"+ port);
                InetAddress serverAddr = InetAddress.getByName(host);
                mSock = new Socket(serverAddr,port);
            } catch (Exception e) {
                Log.i(constants.APP_TAG, e.toString());
                return false;
            }
            if (mSock.isConnected())
            {
                Message msg = mHandler.obtainMessage(constants.MESSAGE_CONNECTED);
                Bundle bundle = new Bundle();
                bundle.putString(constants.MESSAGE_INFO, "Successfully connected to board "+mHost);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return true;
            }
            else
            {
                Log.e(constants.APP_TAG, "Could not connect to board");
                mSock = null;
                return false;
            }

        }

        @Override
        public void run()
        {
            while (!this.isInterrupted())
            {
                while (!connectToHost(mHost, mPort))
                {
                    try {
                        Thread.sleep(constants.LOCAL_BOARD_RECONNECT_TIME);

                    } catch (InterruptedException e) {
                        Log.e(constants.APP_TAG, e.toString());
                    }
                }
                while (mSock != null && mSock.isConnected())
                {
                    try {
                        InputStream in = mSock.getInputStream();
                        DataInputStream dis = new DataInputStream(in);

                        if (dis.available() > 0) {
                            int len = dis.readInt();
                            byte[] buf = new byte[len];
                            if (len > 0) {
                                dis.readFully(buf);
                                Message msg = mHandler.obtainMessage(constants.MESSAGE_NEW_MSG);
                                Bundle bundle = new Bundle();
                                bundle.putString(constants.MESSAGE_INFO, "Recv data from " + mHost);
                                bundle.putByteArray(constants.MESSAGE_DATA, buf);
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }
                        }

                        while(mOutQueue.size() > 0)
                        {
                            write(mOutQueue.poll());
                        }

                    } catch (IOException e) {
                        Log.e(constants.APP_TAG, e.toString());
                        mSock = null;
                    }


                }
            }
        }

        protected void sendPkt(byte[] buf) {
            mOutQueue.add(buf);
        }

        public void write(byte[] buf) {
            try {
                OutputStream out = mSock.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                int len = buf.length;
                dos.writeInt(len);
                if (len > 0) {
                    dos.write(buf, 0, len);
                }
            } catch (Exception e) {
                Log.e(constants.APP_TAG, e.toString());
            }
        }
    }
}
