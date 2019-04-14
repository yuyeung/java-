import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JavaBrowser extends Application {
    static com.sun.webkit.network.CookieManager manager = new com.sun.webkit.network.CookieManager();
    static CookieManager managerNet = new CookieManager();
    static CookieStore store;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Browser");
        Scene scene = new Scene(new Browser(), 1600, 900, Color.web("#F8F8FF"));
        stage.setScene(scene);
        scene.getStylesheets().add("BrowserToolbar.css");

        //默认使用的com.sun.webkit.network.CookieManager中的读取方法都是私有的，并且无法对cookie进行设置，所以要想读取cookie，必须使用java.net.CookieManager
        //但java.net.CookieManager不会向webEngine发送cookie，因此不能生效
        //所以采用java.net.CookieManager保存cookie，然后将cookie写入com.sun.webkit.network.CookieManager的方法
        com.sun.webkit.network.CookieManager.setDefault(manager);
        //读取本地cookie文件，将cookie写入com.sun.webkit.network.CookieManager
        MyCookie.getCookies();
        store = managerNet.getCookieStore();
        managerNet.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        //退出程序时将net包的cookieManager的cookie写入本地文件
        stage.setOnCloseRequest(e -> {
            MyCookie.removeCookies();
            for (URI uri : store.getURIs()) {
                for (HttpCookie cookie : store.get(uri)) {
                    String strcookie = cookie.getName() + "=" + cookie.getValue() +
                            "; max-age=" + cookie.getMaxAge() +
                            "; domain=" + cookie.getDomain() +
                            "; path=" + cookie.getPath();
                    MyCookie.putCookies(uri.toString(), strcookie);
                }
            }
            System.exit(0);
        });

        // show stage
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

class Browser extends Region {

    private final HBox toolBar;
    private final static String[] imageFiles = new String[]{
            "img/backward.png",
            "img/home.png",
            "img/favorites.png",
            "img/download.png",
            "img/cookie.png",
            "img/cookie2.png",
            "img/reload.png"
    };
    private final String home = "http://www.baidu.com";
    private ArrayList<String> urls = new ArrayList<>();
    private Hyperlink[] hyperlinks = new Hyperlink[6];
    private Image[] images = new Image[imageFiles.length];
    private static final WebView browser = new WebView();
    static final WebEngine webEngine = browser.getEngine();
    private ComboBox<String> comboBox = new ComboBox<>();
    private TextField inputUrl = new TextField();
    private int currentIndex;
    private Stage box = new Stage();
    private DownloadBox downloadBox = new DownloadBox();
    private static WebHistory history = webEngine.getHistory();
    static Label message = new Label();

    Browser() {
        //apply the styles
        getStyleClass().add("browser");

        //后退按钮
        hyperlinks[0] = new Hyperlink();
        images[0] = new Image(getClass().getResourceAsStream(imageFiles[0]));
        hyperlinks[0].setGraphic(new ImageView(images[0]));
        //后退按钮事件，跳转到上一个打开的网页
        hyperlinks[0].setOnAction((ActionEvent e) -> {
            if (history.getCurrentIndex() > 0)
                history.go(-1);
        });

        //主页按钮
        hyperlinks[1] = new Hyperlink();
        images[1] = new Image(getClass().getResourceAsStream(imageFiles[1]));
        hyperlinks[1].setGraphic(new ImageView(images[1]));
        //主页按钮事件，打开主页，默认是百度
        hyperlinks[1].setOnAction((ActionEvent e) -> webEngine.load(home));

        //刷新按钮
        hyperlinks[5] = new Hyperlink();
        images[5] = new Image(getClass().getResourceAsStream(imageFiles[6]));
        hyperlinks[5].setGraphic(new ImageView(images[5]));
        //刷新按钮事件，重新加载当前网页
        hyperlinks[5].setOnAction((ActionEvent e) -> webEngine.reload());


        //收藏夹按钮，为了美观，采用隐藏comboBox，用hyperlink调出comboBox下拉菜单的方法
        hyperlinks[2] = new Hyperlink();
        images[2] = new Image(getClass().getResourceAsStream(imageFiles[2]));
        hyperlinks[2].setGraphic(new ImageView(images[2]));
        comboBox.setVisible(false);
        comboBox.setMaxWidth(0);
        comboBox.setVisibleRowCount(30);
        //收藏夹按钮事件
        //点击按钮，弹出下拉菜单
        hyperlinks[2].setOnAction((ActionEvent e) -> {
            comboBox.show();
            comboBox.getSelectionModel().select(-1);
        });
        //下拉菜单事件
        comboBox.setOnAction((ActionEvent e) -> {
            //如果点击的是第一个标签，即添加到收藏夹，则将url信息写入本地文件，并添加到ArrayList以便实时显示，然后显示到收藏夹
            if (comboBox.getSelectionModel().getSelectedIndex() == 0) {
                if (!urls.contains(webEngine.getLocation())) {
                    String favorite = webEngine.getTitle();
                    favorite += " " + webEngine.getLocation();
                    Favorite.addUrl(favorite);
                    System.out.println(favorite);
                    Favorite.urls.add(currentIndex, new Favorite(webEngine.getTitle(), webEngine.getLocation()));
                    comboBox.getItems().add(currentIndex + 1, Favorite.urls.get(currentIndex).title);
                    urls.add(currentIndex, Favorite.urls.get(currentIndex).url);
                    currentIndex++;
                } else {//如果收藏重复，消息框会有提示
                    message.setText("重复收藏");
                    new Timer().schedule(new TimeOutMessage(), 3000);
                }
            } else if (comboBox.getSelectionModel().getSelectedIndex() > 0) {//如果点击别的标签，直接跳转到指定url
                webEngine.load(urls.get(comboBox.getSelectionModel().getSelectedIndex() - 1));
            }
        });

        //地址栏
        //地址栏响应事件
        //智能地址栏，如果输入url，则跳转到指定页面，还可以自动补全http://，如果输入的不是url，则默认使用百度对输入值进行搜索
        inputUrl.setOnAction((ActionEvent e) -> {
            String url = inputUrl.getText();
            if (url.startsWith("http"))
                webEngine.load(url);
            else if (url.startsWith("www."))
                webEngine.load("http://" + url);
            else
                webEngine.load("https://www.baidu.com/s?wd=" + url);
        });
        webEngine.setJavaScriptEnabled(true);

        //连接超时事件，检测如果webEngine的状态是running，则启动连接超时检测线程
        final TimeChecker timeChecker = new TimeChecker();
        final Thread[] timer = {new Thread(timeChecker)};
        webEngine.getLoadWorker().stateProperty().addListener(
                (observableValue, oldState, newState) -> {
                    inputUrl.setText(webEngine.getLocation());
                    timeChecker.setUrl(webEngine.getLocation());
                    if ((timer[0].getState() == Thread.State.TERMINATED || timer[0].getState() == Thread.State.NEW) && webEngine.getLoadWorker().getState() == Worker.State.RUNNING) {
                        timer[0] = new Thread(timeChecker);
                        timer[0].start();
                    }
                });

        //cookie功能
        hyperlinks[4] = new Hyperlink();
        images[4] = new Image(getClass().getResourceAsStream(imageFiles[4]));
        images[5] = new Image(getClass().getResourceAsStream(imageFiles[5]));
        hyperlinks[4].setGraphic(new ImageView(images[4]));
        //cookie事件，切换两个cookieManager，如果切换到webkit包的cookieManager，要将net包中的cookieManager包含的cookie写入webkit包的cookieManager
        hyperlinks[4].setOnAction((ActionEvent e) -> {
            if (com.sun.webkit.network.CookieManager.getDefault().equals(JavaBrowser.manager)) {
                com.sun.webkit.network.CookieManager.setDefault(JavaBrowser.managerNet);
                hyperlinks[4].setGraphic(new ImageView(images[5]));
                System.out.println("Net");
            } else {
                for (URI uri : JavaBrowser.store.getURIs()) {
                    for (HttpCookie cookie : JavaBrowser.store.get(uri)) {
                        String strcookie = cookie.getName() + "=" + cookie.getValue() +
                                "; max-age=" + cookie.getMaxAge() +
                                "; domain=" + cookie.getDomain() +
                                "; path=" + cookie.getPath();
                        Map<String, List<String>> map = new HashMap<>();
                        List<String> list = new ArrayList<>();
                        list.add(strcookie);
                        map.put("Set-Cookie", list);
                        JavaBrowser.manager.put(uri, map);
                    }
                }
                hyperlinks[4].setGraphic(new ImageView(images[4]));
                com.sun.webkit.network.CookieManager.setDefault(JavaBrowser.manager);
                System.out.println("Webkit");
            }
        });

        //下载按钮
        hyperlinks[3] = new Hyperlink();
        images[3] = new Image(getClass().getResourceAsStream(imageFiles[3]));
        hyperlinks[3].setGraphic(new ImageView(images[3]));
        box.setTitle("download");
        Scene scene = new Scene(downloadBox, 900, 600);
        box.setScene(scene);
        scene.getStylesheets().add("BrowserToolbar.css");
        //下载按钮事件，显示下载框，并读取下载地址到下载连接输入框
        hyperlinks[3].setOnAction((ActionEvent e) -> {
            box.show();
            downloadBox.inputUrl.setText(webEngine.getLocation());
        });

        //创建工具栏，添加组件
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(hyperlinks[0], hyperlinks[1], hyperlinks[5]);
        toolBar.getChildren().add(inputUrl);
        toolBar.getChildren().add(message);
        toolBar.getChildren().add(hyperlinks[4]);
        toolBar.getChildren().addAll(hyperlinks[2], comboBox);
        toolBar.getChildren().add(hyperlinks[3]);
//        HBox.setMargin(hyperlinks[3],new Insets(0,0,0,1000));
        HBox.setHgrow(inputUrl, Priority.ALWAYS);
        message.getStyleClass().add("message");

        //加载主页
        webEngine.load(home);

        //加载收藏夹
        comboBox.getItems().add(0, "添加到收藏夹");
        //从本地文件读取url，然后添加到收藏夹
        Favorite.getUrls();
        for (int i = 0; i < Favorite.urls.size(); i++) {
            comboBox.getItems().add(i + 1, Favorite.urls.get(i).title);
            urls.add(i, Favorite.urls.get(i).url);
        }
        currentIndex = Favorite.urls.size();

        //添加组件
        getChildren().add(toolBar);
        getChildren().add(browser);
    }

    //设置工具栏和浏览区域layout
    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double toolBarHeight = toolBar.prefHeight(width);
        layoutInArea(toolBar, 0, 0, width, toolBarHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(browser, 0, toolBarHeight, width, height - toolBarHeight, 0, HPos.CENTER, VPos.CENTER);
    }

}

//下载框，显示下载窗口，进行下载操作
class DownloadBox extends Region {

    private final HBox toolBar;
    private final VBox downloadTasks;
    static Label message = new Label("");
    TextField inputUrl = new TextField();

    DownloadBox() {
        getStyleClass().add("download-win");

        //创建工具栏
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.getStyleClass().add("download-toolbar");

        Image[] images = new Image[3];
        Hyperlink hyperlink = new Hyperlink();
        String[] imageFiles = new String[]{
                "img/add.png",
                "img/start.png",
                "img/pause.png"
        };
        images[0] = new Image(imageFiles[0]);
        hyperlink.setGraphic(new ImageView(images[0]));
        HBox.setHgrow(inputUrl, Priority.ALWAYS);

        images[1] = new Image(imageFiles[1]);
        images[2] = new Image(imageFiles[2]);

        //为下载功能分配线程池
        ExecutorService pool = Executors.newFixedThreadPool(10);
        //下载任务区域
        downloadTasks = new VBox();
        message.setTextFill(Color.RED);

        //下载链接输入框事件
        inputUrl.setOnAction((ActionEvent e) -> {
            String url = inputUrl.getText();
            inputUrl.setText(null);
            //创建下载任务对象
            Download task = new Download(url);
            //如果连接正确，则开始下载，否则显示错误提示
            if (task.right) {
                HBox hBox = new HBox();
                Hyperlink[] start_pause = new Hyperlink[2];
                ProgressIndicator pi = new ProgressIndicator(-1);
                pool.submit(task);
                Progress progress = new Progress(pi, start_pause, task);

                start_pause[0] = new Hyperlink();
                start_pause[0].setGraphic(new ImageView(images[1]));
                start_pause[1] = new Hyperlink();
                start_pause[1].setGraphic(new ImageView(images[2]));
                start_pause[0].setVisible(false);
                //任务开始按钮事件，显示暂停按钮，隐藏开始按钮，设置pause为false，创建下载线程，创建下载进度线程
                start_pause[0].setOnAction(e1 -> {
                    start_pause[1].setVisible(true);
                    start_pause[0].setVisible(false);
                    task.pause = false;
                    progress.pause = false;
                    pool.submit(task);
                    Platform.runLater(() -> new Thread(progress).start());
                });
                //任务暂停按钮事件，显示开始按钮，隐藏暂停按钮，并设置pause为true
                start_pause[1].setOnAction(e1 -> {
                    start_pause[0].setVisible(true);
                    start_pause[1].setVisible(false);
                    task.pause = true;
                    progress.pause = true;
                });

                Label label = new Label(task.fileName);
                label.getStyleClass().add("label");
                hBox.getChildren().addAll(start_pause[0], start_pause[1], label, pi);
                HBox.setHgrow(label, Priority.ALWAYS);
                downloadTasks.getChildren().add(hBox);

                //启动下载进度线程
                Platform.runLater(() -> new Thread(progress).start());
            }
        });

        toolBar.getChildren().addAll(hyperlink, inputUrl, message);

//将工具栏和下载任务区域添加到下载框
        getChildren().add(toolBar);
        getChildren().add(downloadTasks);
    }

//设置工具栏和下载任务区域layout
    @Override
    protected void layoutChildren() {
        double height = getHeight();
        double width = getWidth();
        double toolBarHeight = toolBar.prefHeight(width);
        layoutInArea(toolBar, 0, 0, width, toolBarHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(downloadTasks, 0, toolBarHeight, width, height - toolBarHeight, 0, HPos.CENTER, VPos.CENTER);
    }
}

//显示下载进度
class Progress implements Runnable {
    private ProgressIndicator pi;
    boolean pause = false;
    private Hyperlink[] start_pause;
    private Download task;

    Progress(ProgressIndicator pi, Hyperlink[] start_pause, Download task) {
        this.pi = pi;
        this.start_pause = start_pause;
        this.task = task;
    }

    //当文件下载完成后，将下载进度设置为100%，并且将开始和暂停按钮隐藏
    @Override
    public void run() {
        while (task.running) {
            if (pause)
                break;
            System.out.print("");
        }
        if (!pause)
            Platform.runLater(() -> {
                pi.setProgress(1);
                start_pause[0].setVisible(false);
                start_pause[1].setVisible(false);
            });
    }
}

//连接超时检测
class TimeChecker implements Runnable {
    private String url;

    TimeChecker() {
    }

    void setUrl(String url) {
        this.url = url;
    }

    //设置连接最长时间为3s，如果3s后还未成功加载网页，则显示Time out
    @Override
    public void run() {
        try {
            Thread.sleep(3000);
            Platform.runLater(() -> {
                if (Browser.webEngine.getLocation().equals(url) && Browser.webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED && Browser.webEngine.getTitle() == null) {
//                    Browser.webEngine.load(JavaBrowser.class.getResource("html/index.html").toExternalForm());
                    System.out.println("time out");
                    Browser.message.setText("Time out");
                    new Timer().schedule(new TimeOutMessage(), 3000);//3s后，将Time out隐藏
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

//定时任务，将消息提示框清空
class TimeOutMessage extends TimerTask {
    @Override
    public void run() {
        Platform.runLater(() -> Browser.message.setText(null));
    }
}
