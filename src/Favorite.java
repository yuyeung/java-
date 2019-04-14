import java.io.*;
import java.util.ArrayList;

class Favorite {
    String title, url;
    static ArrayList<Favorite> urls;

    Favorite(String title, String url) {
        this.title = title;
        this.url = url;
    }

    //将收藏夹url写入本地文件
    static void addUrl(String url) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("src/favorites.txt", true));
            pw.println(url);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //读取本地文件的收藏夹信息，放入ArrayList，等待被读取
    static void getUrls() {
        urls = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/favorites.txt"));
            String str;
            while ((str = br.readLine()) != null) {
                Favorite favorite = new Favorite(str.substring(0, str.indexOf(" http")), str.substring(str.indexOf("http"), str.length()));
                urls.add(favorite);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
