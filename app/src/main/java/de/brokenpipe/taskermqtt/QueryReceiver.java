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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class QueryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        final Bundle passThroughMessage = TaskerPlugin.Event.retrievePassThroughData(intent);

        if (passThroughMessage == null) {
            // Tasker Kickstart event; launch service if it isn't running
            Intent connectionService = new Intent(context, MqttConnectionService.class);
            context.startService(connectionService);

            setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            return;
        }

        final String occurredEventSource = passThroughMessage.getString(MqttConnectionService.MQTT_EVENT_SOURCE);
        final String expectedEventSource = localeBundle.getString(BundleExtraKeys.EVENT_SOURCE);

        if (occurredEventSource == null || expectedEventSource == null ||
                !occurredEventSource.equals(expectedEventSource)) {
            setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            return;
        }

        switch (occurredEventSource) {
            case BundleExtraKeys.EVENT_SOURCE_MESSAGE_RECEIVED:
                handleMessageReceived(intent, localeBundle, passThroughMessage);
                break;

            case BundleExtraKeys.EVENT_SOURCE_CONNECTED:
            case BundleExtraKeys.EVENT_SOURCE_DISCONNECTED:
                setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
                break;
        }
    }

    private void handleMessageReceived(Intent intent, Bundle localeBundle, Bundle passThroughMessage) {
        final String filterTopic = localeBundle.getString(BundleExtraKeys.TOPIC);
        final String filterPayload = localeBundle.getString(BundleExtraKeys.PAYLOAD);

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
