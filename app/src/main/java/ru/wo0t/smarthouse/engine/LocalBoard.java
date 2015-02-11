package ru.wo0t.smarthouse.engine;

import android.os.AsyncTask;
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

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/9/15.
 */
public class LocalBoard extends board {
    private String mIpAddr;
    private tcpClient mClient;

    public LocalBoard(BOARD_TYPE type, int id, String name, String ipAddr) {
        super(type, id);
        mIpAddr = ipAddr;
        mClient = new tcpClient(mHandler,ipAddr,constants.REMOTE_BOARD_PORT);
        mClient.execute();
    }

    private void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        tcpClient cl;
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

    private class tcpClient extends AsyncTask<Object, Void, Void> {
        Socket mSock;
        String mHost;
        int mPort;
        Handler mHandler;
        tcpClient(Handler handler,String host, int port) {
            mHandler = handler;
            mHost = host;
            mPort = port;
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
        protected Void doInBackground(Object... params)
        {
            while (!this.isCancelled())
            {
                while (!connectToHost(mHost, mPort))
                {
                    try {
                        Thread.sleep(5000);

                    } catch (InterruptedException e) {
                        Log.e(constants.APP_TAG, e.toString());
                    }
                }
                while (mSock != null && mSock.isConnected())
                {
                    try {
                        InputStream in = mSock.getInputStream();
                        DataInputStream dis = new DataInputStream(in);

                        int len = dis.readInt();
                        byte[] buf = new byte[len];
                        if (len > 0) {
                            dis.readFully(buf);
                            Message msg = mHandler.obtainMessage(constants.MESSAGE_NEW_MSG);
                            Bundle bundle = new Bundle();
                            bundle.putString(constants.MESSAGE_INFO, "Recv data from "+mHost);
                            bundle.putByteArray(constants.MESSAGE_DATA, buf);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    } catch (IOException e) {
                        Log.e(constants.APP_TAG, e.toString());
                        mSock = null;
                    }


                }
            }
            return null;
        }

        public void sendPkt(byte[] buf) {
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
