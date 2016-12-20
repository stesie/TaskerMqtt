package de.brokenpipe.taskermqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class QueryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        final String filterTopic = localeBundle.getString(BundleExtraKeys.FILTER_TOPIC);
        final String filterPayload = localeBundle.getString(BundleExtraKeys.FILTER_PAYLOAD);

        final Bundle passThroughMessage = TaskerPlugin.Event.retrievePassThroughData(intent);

        if (passThroughMessage == null) {
            // Tasker Kickstart event; launch service if it isn't running
            Intent connectionService = new Intent(context, MqttConnectionService.class);
            context.startService(connectionService);

            setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            return;
        }

        final String messageTopic = passThroughMessage.getString(MqttConnectionService.MQTT_TOPIC);
        final String messagePayload = passThroughMessage.getString(MqttConnectionService.MQTT_PAYLOAD);

        if (TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
            Bundle varsBundle = new Bundle();
            varsBundle.putString("%mqtopic", messageTopic);
            varsBundle.putString("%mqpayload", messagePayload);
            TaskerPlugin.addVariableBundle(getResultExtras(true), varsBundle);
        }

        if (filterTopic != null && !filterTopic.isEmpty() && !filterTopic.equals(messageTopic)) {
            setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            return;
        }

        if (filterPayload != null && !filterPayload.isEmpty() && !filterPayload.equals(messagePayload)) {
            setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            return;
        }

        setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
    }
}
