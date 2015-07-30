package me.arendsen.alex.textforward;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by copper on 7/27/15.
 */
public class BadgerMessage {

    public static Pattern BannerPattern = Pattern.compile("^\\s*BADGER/(\\d+)\\.(\\d+)\\s+(.+)\\s*$");
    public static Pattern HeaderPattern = Pattern.compile("^\\s*(.+):\\s+(.+)\\s*$");

    private String command, message;
    private HashMap<String, String> headers;
    private int majorVersion, minorVersion;

    public BadgerMessage(String src) {

        this.headers = new HashMap<>();
        this.message = "";

        boolean parseOK = true;
        boolean inBody = false;
        String[] lines = src.split("[\\r\\n]+");
        if(lines.length > 0) {
            Matcher banner = BannerPattern.matcher(lines[0]);
            if(banner.find()) {
                this.majorVersion = Integer.parseInt(banner.group(1));
                this.minorVersion = Integer.parseInt(banner.group(2));
                this.command = banner.group(3);
            } else {
                parseError();
                parseOK = false;
            }

            if(parseOK && lines.length > 1) {
                for(int i=1; i<lines.length; ++i) {
                    String l = lines[i];
                    if(l.equals("%")) {break;}

                    if(inBody && !l.isEmpty()) {
                        // Append to message
                        this.message += l+"\n";
                    } else {
                        if(l.equals("BODY")) {
                            inBody = true;
                            continue;
                        }

                        Matcher header = HeaderPattern.matcher(l);
                        if(header.find()) {
                            this.headers.put(header.group(1), header.group(2));
                        } else {
                            parseError();
                            break;
                        }
                    }
                }
            }

        } else {parseError();}
    }

    public BadgerMessage(String command, HashMap<String, String> headers, String message) {

        if(command==null) {command = "MALFORMED";}
        if(headers==null) {headers = new HashMap<>();}
        if(message==null) {message = "";}

        this.majorVersion = 1;
        this.minorVersion = 0;
        this.command = command;
        this.headers = headers;
        this.message = message;
    }

    public String getCommand() {
        return this.command;
    }

    public String getHeader(String key) {
        return this.headers.get(key);
    }

    public String getMessage() {
        return this.message;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }


    public void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void removeHeader(String key) {
        if(this.headers.containsKey(key)) {
            this.headers.remove(key);
        }
    }

    public String toString() {
        String out = "BADGER/"+this.majorVersion+"."+this.minorVersion+" "+this.command;
        for(String key : this.headers.keySet()) {
            out += "\n"+key+": "+this.headers.get(key);
        }
        if(this.message != null && !this.message.isEmpty()) {
            out += "\nBODY\n"+this.message;
        }
        out += "\n%";
        return out;
    }

    public String summary() {
        String out = "";
        out += "Major: "+this.majorVersion;
        out += "\nMinor: "+this.minorVersion;
        out += "\nCommand: "+this.command;
        out += "\nHeaders: "+this.headers.size();
        for(String key : this.headers.keySet()) {
            out += "\n\t"+key+": "+this.headers.get(key);
        }
        out += "\nMessage: "+this.message;

        return out;
    }

    private void parseError() {
        this.majorVersion = 1;
        this.minorVersion = 0;
        this.command = "MALFORMED";
        this.headers = new HashMap<>();
        this.message = "";
    }
}
