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
        super(context, type, id, name);
        mIpAddr = ipAddr;
        mClient = new tcpClient(ipAddr,constants.LOCAL_BOARD_PORT);
        mClient.start();
    }

    public String getIpAddr() { return mIpAddr; }

    public void sendPkt(byte[] pkt) {
        if (pkt.length == 0) return;
        tcpClient cl;
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

    private class tcpClient extends Thread {
        Socket mSock;
        String mHost;
        int mPort;
        Queue<byte[]> mOutQueue;

        tcpClient(String host, int port) {
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
                onBoardConnected();
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
                        e.printStackTrace();
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
                                messageParser(new String(buf), null);
                            }
                        }

                        while(mOutQueue.size() > 0)
                        {
                            write(mOutQueue.poll());
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        mSock = null;
                    }


                }
            }
            try {
                mSock.close();
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }
}
