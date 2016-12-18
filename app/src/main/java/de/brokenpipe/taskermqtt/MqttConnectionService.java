package de.brokenpipe.taskermqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnectionService extends Service {
    private static final String TAG = "MqttConnectionService";
    private MqttClient mqttClient;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* String message;

        if (mqttClient == null) {
            message = "failed to start service";
        }
        else if (mqttClient.isConnected()) {
            message = "MQTT Connection established";
        }
        else {
            message = "Not connected";
        }


        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show(); */

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "starting service");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String host = prefs.getString("mqtt_host", null);
        final String clientId = prefs.getString("mqtt_client_id", null);

        if (host == null) {
            Log.e(TAG, "mqtt target host not configured");
            this.stopSelf();
            return;
        }

        if (clientId == null) {
            Log.e(TAG, "mqtt client id not configured");
            this.stopSelf();
            return;
        }

        final int port = Integer.parseInt(prefs.getString("mqtt_port", "1883"));
        final String brokerUrl = "tcp://" + host + ":" + port;

        try {
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            Log.e(TAG, "failed to establish mqtt broker connection: " + e.toString());
            this.stopSelf();
            return;
        }

        new EstablishConnection().execute();
    }

    class EstablishConnection extends AsyncTask<Void, Void, Void> {
        private SharedPreferences prefs;

        EstablishConnection() {
            prefs = PreferenceManager.getDefaultSharedPreferences(MqttConnectionService.this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);

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
