package server;

import org.bytedeco.javacv.FrameGrabber;
import redis.clients.jedis.Jedis;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Intern04 on 25/8/2014.
 */
public class TomVideoStreamReceiver {

    private String host;
    private int port;
    private byte[] queueName;
    private Jedis jedis = null;

    public TomVideoStreamReceiver(String host, int port, String queueName){
        this.host = host;
        this.port = port;
        this.queueName = queueName.getBytes();
    }

    public void VideoStreamReceiver() throws IOException, FrameGrabber.Exception, InterruptedException {

        // ffmpeg -f image2pipe -codec mjpeg -i pipe:0 -f mpegts "udp://localhost:7777"
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Users\\Tom.fu\\Downloads\\ffmpeg-20140824-git-1aa153d-win64-static\\bin\\ffmpeg.exe",
                "-f", "image2pipe", "-codec", "mjpeg", "-i", "pipe:0", "-r", "25", "-f", "mpegts", "\"udp://localhost:7777\"");
        pb.redirectErrorStream(true);
        //pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();

        new Thread("Webcam Process ErrorStream Consumer") {
            public void run() {
                InputStream i = p.getInputStream();
                try {
                    byte[] buf = new byte[1024];
                    while (!isInterrupted()) {
                        i.read(buf);
                    }
                } catch (IOException e) {
                }
            }
        }.start();

        Jedis jedis = getConnectedJedis();
        byte[] baData = null;
        OutputStream ffmpegInput = p.getOutputStream();
        int x = 0;
        long ts = System.currentTimeMillis();
        while (true) {
            try {

                baData = jedis.lpop(queueName);

                if (baData != null) {
                    BufferedImage bufferedImageRead = ImageIO.read(new ByteArrayInputStream(baData));
                    ImageIO.write(bufferedImageRead, "JPEG", ffmpegInput);
                    x++;
                    System.out.println(x);
                }
            } catch (Exception e) {
                System.out.println(e.getStackTrace());
                disconnect();
            }
        }
    }

    public static void main(String args[]) {

        TomVideoStreamReceiver tvsr = new TomVideoStreamReceiver("192.168.0.30", 6379, "tomQ");
        try {
            tvsr.VideoStreamReceiver();
        } catch (Exception e)
        {}
    }

    private Jedis getConnectedJedis() {
        if (jedis != null) {
            return jedis;
        }
        //try connect to redis server
        try {
            jedis = new Jedis(host, port);
        } catch (Exception e) {
        }
        return jedis;
    }

    private void disconnect() {
        try {
            jedis.disconnect();
        } catch (Exception e) {
        }
        jedis = null;
    }



}
