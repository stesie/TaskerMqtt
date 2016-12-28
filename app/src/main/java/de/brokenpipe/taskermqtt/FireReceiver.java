package de.brokenpipe.taskermqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class FireReceiver extends BroadcastReceiver {
    private static final String TAG = "FireReceiver";

    /**
     * @param context {@inheritDoc}.
     * @param intent  the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     *                should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     *                {@link EditActionActivity} and later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Log.e(TAG, String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction()));
            return;
        }

        final Bundle localeBundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        if (localeBundle == null) {
            Log.e(TAG, "Received Intent without EXTRA_BUNDLE");
            return;
        }

        Log.d(TAG, "FireReceiver: delegating intent");
        Intent delegateIntent = new Intent(context, MqttConnectionService.class);
        delegateIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, localeBundle);
        context.startService(delegateIntent);
    }
}
