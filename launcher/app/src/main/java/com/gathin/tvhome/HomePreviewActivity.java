package com.gathin.tvhome;

import android.os.Bundle;
import android.widget.Toast;

/**
 * Opt-in preview of the ORIGINAL CleanYourTV Home dock and focus engine.
 *
 * It deliberately has no HOME intent filter. The owner's TCL keeps its stock
 * Google TV default Home, and the native Concept B v5 dream stays untouched.
 */
public final class HomePreviewActivity extends MainActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Toast.makeText(this, "CleanYourTV Home preview — Back to exit; Home stays Google TV",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        // The real launcher correctly ignores Back on HOME, but a deliberately
        // opened PREVIEW needs a reliable way to return to Google TV.
        finish();
    }
}
