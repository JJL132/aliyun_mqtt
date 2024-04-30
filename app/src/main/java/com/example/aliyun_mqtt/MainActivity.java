package com.example.aliyun_mqtt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity<RemoteMessage> extends AppCompatActivity {

    private MqttClient client;
    private MqttConnectOptions options;
    private Handler handler;
    private ScheduledExecutorService scheduler;
    private String productKey = "a1vuHXu8Hxt";
    private String deviceName = "STM32";
    private String deviceSecret = "eacfc9873a7078098b569c2deb02bb09";
  //  private final String pub_topic = "/sys/a1vuHXu8Hxt/STM32/thing/event/property/post";
    private final String sub_topic = "/sys/a1vuHXu8Hxt/STM32/thing/service/property/set";

 private int temperature =0;
    private int humidity =0;
    private int light=0;
    private int water=0;
    private boolean ren1=false;
    private boolean ren2=false;
    private boolean yan=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv_temp = findViewById(R.id.tv_temp);
        TextView tv_humi = findViewById(R.id.tv_humi);
        TextView tv_lig = findViewById(R.id.tv_lig);
        TextView tv_wat = findViewById(R.id.tv_wat);
        TextView tv_ren1 = findViewById(R.id.tv_ren1);
        TextView tv_ren2 = findViewById(R.id.tv_ren2);
        TextView tv_yan = findViewById(R.id.tv_yan);

        mqtt_init();
        start_reconnect();
        handler = new Handler() {
            @SuppressLint({"SetTextI18n", "HandlerLeak"})
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传
                        break;
                    case 3:  //MQTT 收到消息回传
                        // UTF8Buffer msg=new UTF8Buffer(object.toString());
                        String message = msg.obj.toString();
                        Log.d("MQTT", "handleMessage: " + message);
                        try {
                            JSONObject jsonObjectALL = new JSONObject(message);
                            JSONObject items = jsonObjectALL.getJSONObject("items");
                            // 从items中解析每个字段
                            int temperature = items.getJSONObject("temp").getInt("value");
                            int humidity = items.getJSONObject("humi").getInt("value");
                            int light = items.getJSONObject("lig").getInt("value");
                            int water = items.getJSONObject("wat").getInt("value");
                            boolean ren1 = items.getJSONObject("ren1").getBoolean("value");
                            boolean ren2 = items.getJSONObject("ren2").getBoolean("value");
                            boolean yan = items.getJSONObject("yan").getBoolean("value");
                            // 更新UI
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_temp.setText(String.valueOf(temperature) + " ℃");
                                    tv_humi.setText(String.valueOf(humidity) + " %");
                                    tv_lig.setText(String.valueOf(light) + " Lux");
                                    tv_wat.setText(String.valueOf(water) +"%");
                                    tv_ren1.setText(ren1 ? "有" : "无");
                                    tv_ren2.setText(ren2 ? "有" : "无");
                                    tv_yan.setText(yan ? "有" : "无");
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 30:
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        break;
                    case 31:
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        try {
                            client.subscribe(sub_topic, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void mqtt_init() {
        try {

            String clientId = "a1vuHXu8Hxt.STM32";
            Map<String, String> params = new HashMap<String, String>(16);
            params.put("productKey", productKey);
            params.put("deviceName", deviceName);
            params.put("clientId", clientId);
            String timestamp = String.valueOf(System.currentTimeMillis());
            params.put("timestamp", timestamp);
            // cn-shanghai
            String host_url ="tcp://"+ productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";
            //8883、1883
            String client_id = clientId + "|securemode=2,signmethod=hmacsha1,timestamp=" + timestamp + "|";
            String user_name = deviceName + "&" + productKey;
            String password = com.example.aliyun_mqtt.AliyunIoTSignUtil.sign(params, deviceSecret, "hmacsha1");

            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            System.out.println(">>>" + host_url);
            System.out.println(">>>" + client_id);

            //connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);

            client = new MqttClient(host_url, client_id, new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);
            //设置连接的用户名
            options.setUserName(user_name);
            //设置连接的密码
            options.setPassword(password.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(60);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) throws Exception {
                    // 输出接收到的主题和消息内容到日志
                    Log.d("MQTT", "Received message from topic: " + topicName);
                    Log.d("MQTT", "Message content: " + new String(message.getPayload()));

                    // 封装收到的消息，并发送到 Handler 进行处理
                    Message msg = new Message();
                    msg.what = 3;   // 表示收到消息
                    msg.obj = message.toString(); // 这里根据实际情况选择消息内容的处理方式
                    handler.sendMessage(msg);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!(client.isConnected()))  //如果还未连接
                    {
                        client.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        // 没有用到obj字段
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    // 没有用到obj字段
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
    private void start_reconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    private void subscribe_message() {
        // 检查client是否为null或者未连接状态，如果是则直接返回
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            // 尝试订阅指定的主题sub_topic
            client.subscribe(sub_topic);
        } catch (MqttException e) {
            // 如果订阅过程中发生异常，捕获MqttException并打印异常堆栈信息
            e.printStackTrace();
        }
    }


  /*  private void publish_message(String message) {  // 检查client是否为null或者未连接状态，如果是则直接返回
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage mqtt_message = new MqttMessage();   // 创建一个新的MqttMessage对象
        mqtt_message.setPayload(message.getBytes());    // 将传入的消息内容转换为字节数组，并设置到mqtt_message中
        try { client.publish(pub_topic, mqtt_message); }    // 尝试发布消息到指定的主题pub_topic
        catch (MqttException e) // 如果发布过程中发生异常，捕获MqttException并打印异常堆栈信息
        {
            e.printStackTrace();
        }
    }*/
}
