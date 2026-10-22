package com.gathin.tvhome;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

/**
 * A safe ordinary-app preview of the original CleanYourTV launcher.
 * Not an Android HOME handler and never starts the photo DreamService.
 */
public final class HomePreviewActivity extends MainActivity {
    private static final int DOCK_SLOTS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Toast.makeText(this, "CleanYourTV Home preview — Back exits; Home stays Google TV",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Upstream Home ignores Back because a real HOME app cannot be dismissed.
        // A manually launched preview should first restore dock focus from the
        // app grid, and only finish when the user is already at the dock/top bar.
        RecyclerView grid = findViewById(R.id.grid);
        if (grid != null) {
            View focused = grid.findFocus();
            RecyclerView.ViewHolder focusedHolder =
                    focused == null ? null : grid.findContainingViewHolder(focused);
            if (focusedHolder != null
                    && focusedHolder.getBindingAdapterPosition() >= DOCK_SLOTS) {
                grid.scrollToPosition(0);
                grid.post(() -> {
                    RecyclerView.ViewHolder dock = grid.findViewHolderForAdapterPosition(0);
                    if (dock instanceof AppAdapter.Holder) {
                        ((AppAdapter.Holder) dock).card.requestFocus();
                    }
                });
                return;
            }
        }
        finish();
    }
}
