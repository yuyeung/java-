import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyCookie implements Runnable {
    @Override
    public void run() {
        getCookies();
    }

    //清除本地cookie
    static void removeCookies() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("src/cookies.txt"));
            pw.print("");
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //保存cookie到本地文件
    static void putCookies(String uri, String cookie) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("src/cookies.txt", true));
            pw.println(uri + " " + cookie);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //读取本地cookie，写入两个cookieManager
    static void getCookies() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/cookies.txt"));
            String str, uri, strcookie;
            while ((str = br.readLine()) != null) {
                uri = str.substring(0, str.indexOf(" "));
                strcookie = str.substring(str.indexOf(" ") + 1, str.length());
                Map<String, List<String>> map = new HashMap<>();
                List<String> list = new ArrayList<>();
                list.add(strcookie);
                map.put("Set-Cookie", list);
                JavaBrowser.manager.put(URI.create(uri), map);
                if (!strcookie.contains("max-age=-1"))
                JavaBrowser.managerNet.put(URI.create(uri), map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
