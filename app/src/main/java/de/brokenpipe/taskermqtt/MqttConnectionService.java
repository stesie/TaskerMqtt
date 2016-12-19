package de.brokenpipe.taskermqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.ref.WeakReference;

public class MqttConnectionService extends Service {
    public static final String MQTT_MESSAGE_RECEIVED = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.message_received";
    public static final String MQTT_TOPIC = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.topic";
    public static final String MQTT_PAYLOAD = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.payload";

    static final int MSG_CONNECT = 1;
    static final int MSG_DISCONNECT = 2;
    static final int MSG_SUBSCRIBE = 3;


    private static final String TAG = "MqttConnectionService";
    private MqttClient mqttClient;
    private final Messenger messenger = new Messenger(new IncomingHandler(this));

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    static class IncomingHandler extends Handler {
        private final WeakReference<MqttConnectionService> service;

        IncomingHandler(MqttConnectionService _service) {
            service = new WeakReference<MqttConnectionService>(_service);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "got message" + String.valueOf(msg.what));

            switch (msg.what) {
                case MSG_CONNECT:
                    service.get().connect();
                    break;

                case MSG_DISCONNECT:
                    service.get().disconnect();
                    break;

                case MSG_SUBSCRIBE:
                    try {
                        final String topic = msg.getData().getString(MQTT_TOPIC);
                        Log.d(TAG, "subscribing to " + topic);
                        service.get().mqttClient.subscribe(topic);
                    } catch (MqttException e) {
                        Log.e(TAG, "failed to subscribe: " + e.toString());
                    }
                    break;
            }
        }
    }

    private void connect() {
        if (mqttClient == null) {
            if (!createMqttClient())
                return;
        }

        if (!mqttClient.isConnected())
            new EstablishConnection().execute();
    }

    private boolean createMqttClient() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String host = prefs.getString("mqtt_host", null);
        final String clientId = prefs.getString("mqtt_client_id", null);

        if (host == null) {
            Log.e(TAG, "mqtt target host not configured");
            this.stopSelf();
            return true;
        }

        if (clientId == null) {
            Log.e(TAG, "mqtt client id not configured");
            this.stopSelf();
            return true;
        }

        final int port = Integer.parseInt(prefs.getString("mqtt_port", "1883"));
        final String brokerUrl = "tcp://" + host + ":" + port;

        try {
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            mqttClient.setCallback(new MqttEventHandler());
        } catch (MqttException e) {
            Log.e(TAG, "failed to establish mqtt broker connection: " + e.toString());
            return false;
        }

        return true;
    }

    private void disconnect() {
        if (mqttClient == null)
            return;

        try {
            mqttClient.disconnect();
            Toast.makeText(this, "Disconnected from MQTT Broker", Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            Log.e(TAG, "failed to disconnect from mqtt broker: " + e.toString());
        }
    }

    class MqttEventHandler implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost, triggering reconnect");

            try {
                MqttConnectionService.this.mqttClient.reconnect();
            } catch (MqttException e) {
                Log.e(TAG, "failed to reconnect to mqtt broker: " + e.toString());
                MqttConnectionService.this.stopSelf();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            final String payload = new String(message.getPayload());
            Log.d(TAG, "messageArrived on " + topic + ": " + payload);

            Intent intent = new Intent(MQTT_MESSAGE_RECEIVED);
            intent.putExtra(MQTT_TOPIC, topic);
            intent.putExtra(MQTT_PAYLOAD, payload);
            LocalBroadcastManager.getInstance(MqttConnectionService.this).sendBroadcast(intent);

            Bundle passThroughData = new Bundle();
            passThroughData.putString(MQTT_TOPIC, topic);
            passThroughData.putString(MQTT_PAYLOAD, payload);

            Intent queryIntent = new Intent(com.twofortyfouram.locale.Intent.ACTION_REQUEST_QUERY);
            queryIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_ACTIVITY, EditActivity.class.getName());
            TaskerPlugin.Event.addPassThroughData(queryIntent, passThroughData);
            MqttConnectionService.this.sendBroadcast(queryIntent);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    class EstablishConnection extends AsyncTask<Void, Void, Void> {
        private SharedPreferences prefs;

        EstablishConnection() {
            prefs = PreferenceManager.getDefaultSharedPreferences(MqttConnectionService.this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final MqttConnectOptions options = new MqttConnectOptions();

            if (prefs.getBoolean("mqtt_use_auth", false)) {
                final String username = prefs.getString("mqtt_username", null);
                final String password = prefs.getString("mqtt_password", null);

                if (username == null || username.trim().equals("")) {
                    Log.e(TAG, "mqtt username not set or empty (but auth enabled)");
                    MqttConnectionService.this.stopSelf();
                    return null;
                }

                if (password == null || password.equals("")) {
                    Log.e(TAG, "mqtt password not set");
                    MqttConnectionService.this.stopSelf();
                    return null;
                }

                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            try {
                MqttConnectionService.this.mqttClient.connect(options);
            } catch (MqttException e) {
                Log.e(TAG, "failed to connect to mqtt broker: " + e.toString());
                MqttConnectionService.this.stopSelf();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast toast = Toast.makeText(MqttConnectionService.this, "MQTT Broker connection established", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
