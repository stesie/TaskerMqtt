package de.brokenpipe.taskermqtt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

public class EditActionActivity extends Activity {
    private Spinner actionTypeSpinner;
    private EditText topicText, payloadText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        setContentView(R.layout.edit_action);
        actionTypeSpinner = (Spinner) findViewById(R.id.actionType);
        topicText = (EditText) findViewById(R.id.topic);
        payloadText = (EditText) findViewById(R.id.payload);

        actionTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                topicText.setEnabled(position >= 2);
                payloadText.setEnabled(position == 4);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (savedInstanceState != null || localeBundle == null)
            return;

        final String actionType = localeBundle.getString(BundleExtraKeys.ACTION_TYPE);

        if (actionType == null)
            return;

        switch (actionType) {
            case BundleExtraKeys.ACTION_TYPE_CONNECT:
                this.actionTypeSpinner.setSelection(0);
                break;

            case BundleExtraKeys.ACTION_TYPE_DISCONNECT:
                this.actionTypeSpinner.setSelection(1);
                break;

            case BundleExtraKeys.ACTION_TYPE_SUBSCRIBE:
                this.actionTypeSpinner.setSelection(2);
                break;

            case BundleExtraKeys.ACTION_TYPE_UNSUBSCRIBE:
                this.actionTypeSpinner.setSelection(3);
                break;

            case BundleExtraKeys.ACTION_TYPE_PUBLISH:
                this.actionTypeSpinner.setSelection(4);
                break;
        }

        topicText.setText(localeBundle.getString(BundleExtraKeys.TOPIC, ""));
        payloadText.setText(localeBundle.getString(BundleExtraKeys.PAYLOAD, ""));
    }

    public void finishActivity(View view) {
        final String topicText = this.topicText.getText().toString().trim();
        final String payloadText = this.payloadText.getText().toString().trim();

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        StringBuilder blurb = new StringBuilder();

        blurb.append("Action Type: ");
        blurb.append(actionTypeSpinner.getSelectedItem().toString());
        blurb.append("\n");

        switch (actionTypeSpinner.getSelectedItemPosition()) {
            case 0:
                bundle.putString(BundleExtraKeys.ACTION_TYPE, BundleExtraKeys.ACTION_TYPE_CONNECT);
                break;

            case 1:
                bundle.putString(BundleExtraKeys.ACTION_TYPE, BundleExtraKeys.ACTION_TYPE_DISCONNECT);
                break;

            case 2:
                bundle.putString(BundleExtraKeys.ACTION_TYPE, BundleExtraKeys.ACTION_TYPE_SUBSCRIBE);
                bundle.putString(BundleExtraKeys.TOPIC, topicText);

                blurb.append("Topic: ");
                blurb.append(topicText.equals("") ? "*" : topicText);
                blurb.append("\n");
                break;

            case 3:
                bundle.putString(BundleExtraKeys.ACTION_TYPE, BundleExtraKeys.ACTION_TYPE_UNSUBSCRIBE);
                bundle.putString(BundleExtraKeys.TOPIC, topicText);

                blurb.append("Topic: ");
                blurb.append(topicText.equals("") ? "*" : topicText);
                blurb.append("\n");
                break;

            case 4:
                bundle.putString(BundleExtraKeys.ACTION_TYPE, BundleExtraKeys.ACTION_TYPE_PUBLISH);
                bundle.putString(BundleExtraKeys.TOPIC, topicText);
                bundle.putString(BundleExtraKeys.PAYLOAD, payloadText);

                blurb.append("Topic: ");
                blurb.append(topicText);
                blurb.append("\n");

                blurb.append("Payload: ");
                blurb.append(payloadText);
                break;
        }

        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb.toString());

        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
