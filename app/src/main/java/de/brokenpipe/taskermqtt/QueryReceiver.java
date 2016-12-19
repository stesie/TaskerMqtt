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
        final String messageTopic = passThroughMessage.getString(MqttConnectionService.MQTT_TOPIC);
        final String messagePayload = passThroughMessage.getString(MqttConnectionService.MQTT_PAYLOAD);

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
