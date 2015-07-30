package me.arendsen.alex.textforward;

/**
 * Created by copper on 7/27/15.
 */
public class BadgerMessageTest {

    public static void main(String[] args) {
        System.out.println("\n#################\nMessage 1 -- Simple ACK");
        BadgerMessage bm1 = new BadgerMessage("BADGER/1.1 ACK\n%");
        System.out.println(bm1.summary());
        System.out.println("\n---\n"+bm1.toString());

        System.out.println("\n#################\nMessage 2 -- Message with headers, no body");
        BadgerMessage bm2 = new BadgerMessage("BADGER/7.2 CONNECT\ntype: agent\npassword: mypassword\nhandle: badger\n%");
        System.out.println(bm2.summary());
        System.out.println("\n---\n"+bm2.toString());

        System.out.println("\n#################\nMessage 3 -- Message with headers and body");
        BadgerMessage bm3 = new BadgerMessage("BADGER/1.0 SMS_NEW\nsender: 16167233775\nBODY\nThis is another message\n%");
        System.out.println(bm3.summary());
        System.out.println("\n---\n"+bm3.toString());

        System.out.println("\n#################\nMessage 4 -- Message with body, no headers");
        BadgerMessage bm4 = new BadgerMessage("BADGER/4.2 JUST_BODY\nBODY\nMessage testing\n%");
        System.out.println(bm4.summary());
        System.out.println("\n---\n"+bm4.toString());
    }
}
