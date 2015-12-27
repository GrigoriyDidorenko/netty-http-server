package net.didorenko.netty;

/**
 * package: net.didorenko.netty
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 27.12.2015
 */


//TODO this is my test class to check features, will be deleted in final version
public class hello {

    public static void main(String[] args) {
        String s = "https://www.google.com.ua/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=if(value.contains(%22%253C%22))";

        if(s.contains("~^(?:(?:https?|ftp|telnet)" +
                "://(?:[a-z0-9_-]{1,32}(?::[a-z0-9_-]{1,32})?@)?)?(?:(?:[a-z0-9-]{1,128}\\.)+" +
                "(?:ru|su|com|net|org|mil|edu|arpa|gov|biz|info|aero|inc|name|[a-z]{2})|(?!0)(?:(?!0[^.]|255)[0-9]{1,3}\\.)" +
                "{3}(?!0|255)[0-9]{1,3})(?:/[a-z0-9.,_@%&?+=\\~/-]*)?(?:#[^ '\\\"&]*)?$~i"));
        System.out.println("bingo");
    }
}
