package me.arendsen.alex.textforward;

import android.os.AsyncTask;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by copper on 7/25/15.
 */
public class Server {

    String ipAddress;
    int port;
    Socket sock;
    PrintWriter out;

    public Server(String ipAddress, int port) throws Exception {
        this.ipAddress = ipAddress;
        this.port = port;
        this.sock = null;

        new SocketCreationTask().execute(this);
    }

    public void send(String message, String sender) {
        if(message!=null) {
            out.println("BADGER/1.1 NEW_MESSAGE\nsender: "+sender+"\nBODY\n"+message);
        }
    }

    public void close() {
        try {
            out.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out = null;
            sock = null;
        }
    }

    public boolean isReady() {
        return sock!=null && out!=null;
    }

    private class SocketCreationTask extends AsyncTask<Server, Void, Void> {

        Server server;

        @Override
        protected Void doInBackground(Server... params) {
            this.server = null;
            if(params.length!=1) {
                return null;
            } else {
                this.server = params[0];
                try {
                    this.server.sock = new Socket(
                            this.server.ipAddress,
                            this.server.port
                    );
                    this.server.out = new PrintWriter(this.server.sock.getOutputStream(), true);
                } catch (Exception e) {
                    if(this.server!=null) {
                        this.server.sock = null;
                        this.server.out = null;
                    }
                }
            }
            return null;
        }
    }
}

