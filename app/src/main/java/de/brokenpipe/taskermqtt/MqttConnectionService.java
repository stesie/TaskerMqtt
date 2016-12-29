/*
 * Copyright 2016 Stefan Siegl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.brokenpipe.taskermqtt;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
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
import java.util.LinkedList;
import java.util.Queue;

public class MqttConnectionService extends Service {
    public static final String MQTT_MESSAGE_RECEIVED = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.message_received";

    public static final String MQTT_EVENT_SOURCE = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.eventSource";
    public static final String MQTT_TOPIC = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.topic";
    public static final String MQTT_PAYLOAD = "de.brokenpipe.taskermqtt.backend.MqttConnectionService.payload";

    static final int MSG_CONNECT = 1;
    static final int MSG_DISCONNECT = 2;
    static final int MSG_SUBSCRIBE = 3;

    private static final String TAG = "MqttConnectionService";
    private MqttClient mqttClient;
    private final Messenger messenger = new Messenger(new IncomingHandler(this));

    Queue<AsyncTask<Void, Void, Void>> todos = new LinkedList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        if (localeBundle == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        final String actionType = localeBundle.getString(BundleExtraKeys.ACTION_TYPE);

        if (actionType != null)
            switch (actionType) {
                case BundleExtraKeys.ACTION_TYPE_CONNECT:
                    connect();
                    break;

                case BundleExtraKeys.ACTION_TYPE_DISCONNECT:
                    disconnect();
                    break;

                case BundleExtraKeys.ACTION_TYPE_SUBSCRIBE:
                    subscribe(localeBundle.getString(BundleExtraKeys.TOPIC));
                    break;

                case BundleExtraKeys.ACTION_TYPE_UNSUBSCRIBE:
                    unsubscribe(localeBundle.getString(BundleExtraKeys.TOPIC));
                    break;

                case BundleExtraKeys.ACTION_TYPE_PUBLISH:
                    publish(localeBundle.getString(BundleExtraKeys.TOPIC), localeBundle.getString(BundleExtraKeys.PAYLOAD));
                    break;
            }

        return super.onStartCommand(intent, flags, startId);
    }

    private void publish(String topic, String payload) {
        runTask(new PublishMessage(topic, payload), true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    static class IncomingHandler extends Handler {
        private final WeakReference<MqttConnectionService> service;

        IncomingHandler(MqttConnectionService _service) {
            service = new WeakReference<>(_service);
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
                    final String topic = msg.getData().getString(MQTT_TOPIC);
                    service.get().subscribe(topic);
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

    private void subscribe(String topic) {
        runTask(new SubscribeTopic(topic), true);
    }

    private void unsubscribe(String topic) {
        runTask(new UnsubscribeTopic(topic), false);
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
            Bundle passThroughData = new Bundle();
            passThroughData.putString(MQTT_EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_DISCONNECTED);
            broadcastEventQuery(passThroughData);

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
            passThroughData.putString(MQTT_EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_MESSAGE_RECEIVED);
            passThroughData.putString(MQTT_TOPIC, topic);
            passThroughData.putString(MQTT_PAYLOAD, payload);
            broadcastEventQuery(passThroughData);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    private void broadcastEventQuery(Bundle passThroughData) {
        Intent queryIntent = new Intent(com.twofortyfouram.locale.Intent.ACTION_REQUEST_QUERY);
        queryIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_ACTIVITY, EditEventActivity.class.getName());
        TaskerPlugin.Event.addPassThroughData(queryIntent, passThroughData);
        MqttConnectionService.this.sendBroadcast(queryIntent);
    }

    class SubscribeTopic extends AsyncTask<Void, Void, Void> {
        private String topic;

        SubscribeTopic (String topic) {
            this.topic = topic;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mqttClient.subscribe(topic);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to subscribe to topic: " + topic);
            }
            return null;
        }

        @Override
        @MainThread
        protected void onPostExecute(Void aVoid) {
            processNextTask();
        }
    }

    class UnsubscribeTopic extends AsyncTask<Void, Void, Void> {
        private String topic;

        UnsubscribeTopic (String topic) {
            this.topic = topic;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to unsubscribe from topic: " + topic);
            }
            return null;
        }

        @Override
        @MainThread
        protected void onPostExecute(Void aVoid) {
            processNextTask();
        }
    }

    class PublishMessage extends AsyncTask<Void, Void, Void> {
        private String topic;
        private String payload;

        PublishMessage (String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
            } catch (MqttException e) {
                Log.e(TAG, "Failed to publish message on " + topic);
            }
            return null;
        }

        @Override
        @MainThread
        protected void onPostExecute(Void aVoid) {
            processNextTask();
        }
    }

    private void runTask(AsyncTask<Void, Void, Void> task, boolean connectImplicitly) {
        if (todos.isEmpty()) {
            if (mqttClient != null && mqttClient.isConnected()) {
                task.execute();
                return;
            }

            if (connectImplicitly) {
                Log.d(TAG, "currently not connected, connecting implicitly");
                connect();
            }
            else {
                Log.d(TAG, "not connected, rejecting task");
                return;
            }
        }

        todos.add(task);
    }

    private void processNextTask() {
        if (todos.isEmpty())
            return;

        AsyncTask<Void, Void, Void> next = todos.remove();
        next.execute();
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

                Bundle passThroughData = new Bundle();
                passThroughData.putString(MQTT_EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_CONNECTED);
                broadcastEventQuery(passThroughData);
            } catch (MqttException e) {
                Log.e(TAG, "failed to connect to mqtt broker: " + e.toString());
                MqttConnectionService.this.stopSelf();
            }

            return null;
        }

        @Override
        @MainThread
        protected void onPostExecute(Void aVoid) {
            Toast toast = Toast.makeText(MqttConnectionService.this, "MQTT Broker connection established", Toast.LENGTH_SHORT);
            toast.show();
            processNextTask();
        }
    }
}
