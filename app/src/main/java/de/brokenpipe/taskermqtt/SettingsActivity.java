package de.brokenpipe.taskermqtt;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new TaskerMqttPreferenceFragment())
                .commit();
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || TaskerMqttPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class TaskerMqttPreferenceFragment extends PreferenceFragment {
        Intent connectionService;
        BroadcastReceiver messageReceiver;
        Messenger serviceMessenger = null;

        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceMessenger = new Messenger(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        @Override
        public void onStop() {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
            super.onStop();
        }

        @Override
        public void onStart() {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
                    new IntentFilter(MqttConnectionService.MQTT_MESSAGE_RECEIVED));
            super.onStart();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            bindPreferenceSummaryToValue(findPreference("mqtt_host"));
            bindPreferenceSummaryToValue(findPreference("mqtt_port"));
            bindPreferenceSummaryToValue(findPreference("mqtt_client_id"));
            bindPreferenceSummaryToValue(findPreference("mqtt_username"));

            connectionService = new Intent(getActivity(), MqttConnectionService.class);
            getActivity().bindService(connectionService, serviceConnection, Context.BIND_AUTO_CREATE);

            findPreference("mqtt_connect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sendToService(MqttConnectionService.MSG_CONNECT, null);
                    return true;
                }
            });

            findPreference("mqtt_disconnect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sendToService(MqttConnectionService.MSG_DISCONNECT, null);
                    return true;
                }
            });

            findPreference("mqtt_subscribe").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final EditText topic = new EditText(getActivity());

                    new AlertDialog.Builder(getActivity())
                            .setTitle("Subscribe Topic")
                            .setMessage("Topic (or pattern) to subscribe to")
                            .setView(topic)
                            .setPositiveButton("Subscribe", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString(MqttConnectionService.MQTT_TOPIC, topic.getText().toString());
                                    sendToService(MqttConnectionService.MSG_SUBSCRIBE, bundle);
                                }
                            })
                    .show();
                    return true;
                }
            });

            messageReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = "MQTT Message on " + intent.getStringExtra(MqttConnectionService.MQTT_TOPIC)
                            + ": " + intent.getStringExtra(MqttConnectionService.MQTT_PAYLOAD);
                    Toast toast = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
                    toast.show();
                }
            };
        }

        private void sendToService(int what, Bundle bundle) {
            Message msg = Message.obtain(null, what);
            msg.setData(bundle);

            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
