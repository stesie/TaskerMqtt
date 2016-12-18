package de.brokenpipe.taskermqtt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class EditActivity extends Activity {
    private EditText filterTopicText, filterPayloadText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        setContentView(R.layout.edit);
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

        // Tasker's variable replacement
        //if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this))
        //    TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{BundleExtraKeys.TOPIC, BundleExtraKeys.PAYLOAD});

        // We define the blurb that will appear in the configuration
        StringBuilder blurb = new StringBuilder();

        blurb.append("Topic: ");
        blurb.append(topicText.equals("") ? "*" : topicText);
        blurb.append("\n");

        blurb.append("Payload: ");
        blurb.append(payloadText.equals("") ? "*" : payloadText);

        resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb.toString());

        setResult(RESULT_OK, resultIntent);
        super.finish();
    }
}
