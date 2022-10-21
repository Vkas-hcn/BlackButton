package com.blackbutton.fast.tool.secure.utils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Ping {
    private static final String TAG = "Ping";
    private static Runtime runtime;
    private static Process process;
    private static File pingFile;
    /**
     * Jdk1.5的InetAddresss,代码简单
     * @param ipAddress
     * @throws Exception
     */
    public static boolean ping(String ipAddress) throws Exception {
        int timeOut = 3000; // 超时应该在3钞以上
        boolean status = InetAddress.getByName(ipAddress).isReachable(timeOut); // 当返回值是true时，说明host是可用的，false则不可。
        return status;
    }
    /**
     * 使用java调用cmd命令,这种方式最简单，可以把ping的过程显示在本地。ping出相应的格式
     * @param url
     * @throws Exception
     */
    public static void ping1(String url) throws Exception {
        String line = null;
        // 获取主机名
        URL transUrl = null;
        String filePathName = "/sdcard/" + "/ping";
        File commonFilePath = new File(filePathName);
        if (!commonFilePath.exists()) {
            commonFilePath.mkdirs();
            Log.w(TAG, "create path: " + commonFilePath);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = df.format(new Date());
        String file = "result" + date + ".txt";
        pingFile = new File(commonFilePath,file);
        try {
            transUrl = new URL(url);
            String hostName = transUrl.getHost();
            Log.e(TAG, "hostName: " + hostName);
            runtime = Runtime.getRuntime();
            process = runtime.exec("ping " + hostName);
            BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int k = 0;
            while ((line = buf.readLine()) != null) {
                if (line.length() > 0 && line.indexOf("time=") > 0) {
                    String context = line.substring(line.indexOf("time="));
                    int index = context.indexOf("time=");
                    String str = context.substring(index + 5, index + 9);
                    Log.e(TAG, "time=: " + str);
                    String result =
                            null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date()) + ", " + hostName + ", " + str + "rn";
                    }
                    Log.e(TAG, "result: " + result);
                    write(pingFile, result);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    /**
     * 使用java调用控制台的ping命令，这个比较可靠，还通用，使用起来方便：传入个ip，设置ping的次数和超时，就可以根据返回值来判断是否ping通。
     */
    public static boolean ping2(String ipAddress, int pingTimes, int timeOut) {
        BufferedReader in = null;
        // 将要执行的ping命令,此命令是windows格式的命令
        Runtime r = Runtime.getRuntime();
        String pingCommand = "ping " + ipAddress + " -n " + pingTimes + " -w " + timeOut;
        try {
            // 执行命令并获取输出
            System.out.println(pingCommand);
            Process p = r.exec(pingCommand);
            if (p == null) {
                return false;
            }
            // 逐行检查输出,计算类似出现=23ms TTL=62字样的次数
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int connectedCount = 0;
            String line = null;
            while ((line = in.readLine()) != null) {
                connectedCount += getCheckResult(line);
            }
            // 如果出现类似=23ms TTL=62这样的字样,出现的次数=测试次数则返回真
            return connectedCount == pingTimes;
        } catch (Exception ex) {
            ex.printStackTrace(); // 出现异常则返回假
            return false;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 停止运行ping
     */
    public static void killPing() {
        if (process != null) {
            process.destroy();
            Log.e(TAG, "process: " + process);
        }
    }
    public static void write(File file, String content) {
        BufferedWriter out = null;
        Log.e(TAG, "file: " + file);
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // 若line含有=18ms TTL=16字样,说明已经ping通,返回1,否則返回0.
    private static int getCheckResult(String line) { // System.out.println("控制台输出的结果为:"+line);
        Pattern pattern = Pattern.compile("(/d+ms)(/s+)(TTL=/d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            return 1;
        }
        return 0;
    }
    /*
     * public static void main(String[] args) throws Exception { String ipAddress = "appdlssl.dbankcdn.com"; //
     * System.out.println(ping(ipAddress)); ping02(); // System.out.println(ping(ipAddress, 5, 5000)); }
     */
}
