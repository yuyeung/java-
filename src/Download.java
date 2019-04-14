import javafx.application.Platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Download implements Runnable {
    private String url;
    String fileName;
    boolean pause = false, done = true, right = true, running = true;
    private long remoteFileSize;
    private long localFileSize = -1;
    final private String savePath = "download_files/";
    private boolean continueTask = false;

    Download(String url) {
        this.url = url;

        //判断下载连接是否合法
        if (url.contains("/"))
            fileName = url.substring(url.lastIndexOf("/"));
        if (fileName.equals("/")) {
            right = false;
            Platform.runLater(() -> DownloadBox.message.setText("无法下载该文件"));
            new Timer().schedule(new TimerMessage(), 3000);
        }
        //获取文件名
        fileName = fileName.replaceAll("[^a-zA-Z0-9.]", "");

        // 获得远程文件大小
        remoteFileSize = getRemoteFileSize(url);
        if (remoteFileSize == -1) {
            right = false;
            Platform.runLater(() -> DownloadBox.message.setText("文件获取失败"));
            new Timer().schedule(new TimerMessage(), 3000);
        }
        System.out.println("远程文件大小=" + remoteFileSize);
        String tempName = fileName;
        int i = 0;
        File file = new File(savePath + fileName);
        //判断文件续传或是重复下载的情况
        if (file.exists()) {
            localFileSize = file.length();
            System.out.println("已有文件大小为:" + localFileSize);
            if (localFileSize < remoteFileSize) {
                continueTask = true;
                System.out.println("文件续传");
            } else {
                System.out.println("文件存在，重新下载");
                if (tempName.contains("."))
                    do {
                        i++;
                        fileName = tempName.substring(0, tempName.lastIndexOf(".")) + "(" + i +
                                ")" + tempName.substring(tempName.lastIndexOf("."));
                        file = new File(savePath + fileName);
                    } while (file.exists());
            }
        }
    }

    //下载函数，可以响应暂停功能，并且下载错误时会有消息提示
    private void down(String URL, long nPos, String savePathAndFile) {
        try {
            URL url = new URL(URL);
            HttpURLConnection httpConnection =
                    (HttpURLConnection) url.openConnection();
            // 设置断点续传的开始位置
            httpConnection.setRequestProperty("RANGE", "bytes=" + nPos + "-");
            // 获得输入流
            BufferedInputStream input =
                    new BufferedInputStream(httpConnection.getInputStream());
            RandomAccessFile SavedFile = new RandomAccessFile(savePathAndFile, "rw");
            // 定位文件指针到nPos位置
            SavedFile.seek(nPos);
            byte[] b = new byte[1024];
            int nRead;
            // 从输入流中读入字节流，然后写到文件中
            while ((nRead = input.read(b, 0, 1024)) > 0) {
                (SavedFile).write(b, 0, nRead);
                if (pause) {
                    System.out.println("暂停下载");
                    break;
                }
            }
            if (!pause)
                running = false;
            httpConnection.disconnect();
            SavedFile.close();
        } catch (IOException e) {
            System.out.println("下载发生错误");
            Platform.runLater(() -> DownloadBox.message.setText("下载发生错误"));
            done = false;
            new Timer().schedule(new TimerMessage(), 3000);
        }
    }

    //获取远程文件大小
    private long getRemoteFileSize(String url) {
        long size = 0;
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) (new URL(url)).openConnection();
            size = conn.getContentLength();
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("无法连接文件");
        }
        return size;
    }

    //执行下载
    @Override
    public void run() {
        System.out.println("下载中");
        if (continueTask) {
            //不完整，继续下载
            down(url, localFileSize, savePath + fileName);
        } else
            down(url, 0, savePath + fileName);
        if (!pause && done) {
            System.out.println("下载完成");
        }
    }
}

class TimerMessage extends TimerTask {
    @Override
    public void run() {
        Platform.runLater(() -> DownloadBox.message.setText(""));
    }
}