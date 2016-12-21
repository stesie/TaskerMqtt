package de.brokenpipe.taskermqtt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EditEventActivity extends Activity {
    private EditText filterTopicText, filterPayloadText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        setContentView(R.layout.edit_event);
        filterTopicText = (EditText) findViewById(R.id.filter_topic);
        filterPayloadText = (EditText) findViewById(R.id.filter_payload);

        if (savedInstanceState == null && localeBundle != null) {
            filterTopicText.setText(localeBundle.getString(BundleExtraKeys.FILTER_TOPIC));
            filterPayloadText.setText(localeBundle.getString(BundleExtraKeys.FILTER_PAYLOAD));
        }
    }

    public void finishActivity(View view) {
        final String topicText = filterTopicText.getText().toString().trim();
        final String payloadText = filterPayloadText.getText().toString().trim();

        Bundle bundle = new Bundle();
        bundle.putString(BundleExtraKeys.FILTER_TOPIC, topicText);
        bundle.putString(BundleExtraKeys.FILTER_PAYLOAD, payloadText);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);

        // We define the blurb that will appear in the configuration
        StringBuilder blurb = new StringBuilder();

        blurb.append("Topic: ");
        blurb.append(topicText.equals("") ? "*" : topicText);
        blurb.append("\n");

        blurb.append("Payload: ");
        blurb.append(payloadText.equals("") ? "*" : payloadText);

        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb.toString());

        if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
            TaskerPlugin.addRelevantVariableList(resultIntent, new String [] {
                    "%mqtopic\nMQTT Topic\nThe topic the message was received on",
                    "%mqpayload\nMQTT Payload\nThe payload of the message that was received"
            });
        }

        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
