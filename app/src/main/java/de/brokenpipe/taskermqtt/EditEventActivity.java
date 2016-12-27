package de.brokenpipe.taskermqtt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

public class EditEventActivity extends Activity {
    private Spinner eventSourceSpinner;
    private EditText filterTopicText, filterPayloadText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        setContentView(R.layout.edit_event);
        eventSourceSpinner = (Spinner) findViewById(R.id.eventSource);
        filterTopicText = (EditText) findViewById(R.id.filter_topic);
        filterPayloadText = (EditText) findViewById(R.id.filter_payload);

        eventSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean enableFilters = position == 0;
                filterTopicText.setEnabled(enableFilters);
                filterPayloadText.setEnabled(enableFilters);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (savedInstanceState != null || localeBundle == null)
            return;

        final String eventSource = localeBundle.getString(BundleExtraKeys.EVENT_SOURCE);

        if (eventSource == null)
            return;

        switch (eventSource) {
            case BundleExtraKeys.EVENT_SOURCE_MESSAGE_RECEIVED:
                this.eventSourceSpinner.setSelection(0);
                break;

            case BundleExtraKeys.EVENT_SOURCE_CONNECTED:
                this.eventSourceSpinner.setSelection(1);
                break;

            case BundleExtraKeys.EVENT_SOURCE_DISCONNECTED:
                this.eventSourceSpinner.setSelection(2);
                break;
        }

        filterTopicText.setText(localeBundle.getString(BundleExtraKeys.TOPIC, ""));
        filterPayloadText.setText(localeBundle.getString(BundleExtraKeys.PAYLOAD, ""));
    }

    public void finishActivity(View view) {
        final String topicText = filterTopicText.getText().toString().trim();
        final String payloadText = filterPayloadText.getText().toString().trim();

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        StringBuilder blurb = new StringBuilder();

        blurb.append("Event Source: ");
        blurb.append(eventSourceSpinner.getSelectedItem().toString());
        blurb.append("\n");


        switch (eventSourceSpinner.getSelectedItemPosition()) {
            case 0:
                bundle.putString(BundleExtraKeys.EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_MESSAGE_RECEIVED);
                bundle.putString(BundleExtraKeys.TOPIC, topicText);
                bundle.putString(BundleExtraKeys.PAYLOAD, payloadText);

                blurb.append("Topic: ");
                blurb.append(topicText.equals("") ? "*" : topicText);
                blurb.append("\n");

                blurb.append("Payload: ");
                blurb.append(payloadText.equals("") ? "*" : payloadText);

                if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
                    TaskerPlugin.addRelevantVariableList(resultIntent, new String [] {
                            "%mqtopic\nMQTT Topic\nThe topic the message was received on",
                            "%mqpayload\nMQTT Payload\nThe payload of the message that was received"
                    });
                }
                break;

            case 1:
                bundle.putString(BundleExtraKeys.EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_CONNECTED);
                break;

            case 2:
                bundle.putString(BundleExtraKeys.EVENT_SOURCE, BundleExtraKeys.EVENT_SOURCE_DISCONNECTED);
                break;
        }

        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb.toString());

        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
