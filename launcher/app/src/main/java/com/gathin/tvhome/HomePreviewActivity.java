package com.gathin.tvhome;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Ordinary-app preview of the original CleanYourTV Home UI.
 * Never an Android HOME candidate; Back on the dock exits to Google TV.
 *
 * The RecyclerView remains the original focus/scroll implementation. Only
 * its render clip is adjusted while parked on the dock to keep the first
 * screen uncluttered. Press Down to expose the unchanged installed-app grid.
 */
public class HomePreviewActivity extends MainActivity {
    private static final int DOCK_SLOTS = 4;
    private RecyclerView previewGrid;
    private boolean dockOnly = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewGrid = findViewById(R.id.grid);
        if (previewGrid != null) {
            previewGrid.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                    oldLeft, oldTop, oldRight, oldBottom) ->
                    scheduleDockClip());
            previewGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView list, int dx, int dy) {
                    if (dockOnly) scheduleDockClip();
                }
            });
            scheduleDockClip();
        }
        if (isStandalonePreview() && savedInstanceState == null) {
            Toast.makeText(this, "CleanYourTV Home preview — Back exits; Home stays Google TV",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onTileFocused(int position, View card) {
        dockOnly = position >= 0 && position < DOCK_SLOTS;
        if (previewGrid != null && !dockOnly) {
            // Remove the clipping BEFORE the parent scrolls/focuses a grid tile.
            previewGrid.setClipBounds(null);
        }
        super.onTileFocused(position, card);
        scheduleDockClip();
    }

    private void scheduleDockClip() {
        if (previewGrid == null || !dockOnly) return;
        previewGrid.post(this::applyDockClip);
    }

    private void applyDockClip() {
        RecyclerView grid = previewGrid;
        if (grid == null || !dockOnly || isFinishing()) return;
        int width = grid.getWidth(), height = grid.getHeight();
        if (width <= 0 || height <= 0) return;
        // The original DockDecoration measures its charcoal band from the
        // first four real app views. Clip just beyond their row (and band),
        // not over the tiles or their moving glass focus (a sibling view).
        int bottom = 0;
        for (int i = 0; i < DOCK_SLOTS; i++) {
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(i);
            if (holder != null) bottom = Math.max(bottom, holder.itemView.getBottom());
        }
        if (bottom <= 0) {
            // No measured dock yet: never make an empty/black clipped viewport.
            grid.setClipBounds(null);
            return;
        }
        int clearance = Math.round(5 * getResources().getDisplayMetrics().density);
        int clipBottom = Math.min(height, bottom + clearance);
        // Avoid a redundant invalidate for every frame in a smooth dock scroll.
        Rect old = grid.getClipBounds();
        if (old == null || old.right != width || old.bottom != clipBottom) {
            grid.setClipBounds(new Rect(0, 0, width, clipBottom));
        }
    }

    /** True for the independent v7 preview; false for the HOME-capable subclass. */
    protected boolean isStandalonePreview() { return true; }

    @Override
    public void onBackPressed() {
        // The actual HOME launcher ignores Back. This stand-alone preview
        // returns from the app grid to the dock, then exits on another Back.
        RecyclerView grid = findViewById(R.id.grid);
        if (grid != null) {
            View focused = grid.findFocus();
            RecyclerView.ViewHolder holder =
                    focused == null ? null : grid.findContainingViewHolder(focused);
            if (holder != null
                    && holder.getBindingAdapterPosition() >= DOCK_SLOTS) {
                grid.setClipBounds(null);
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
        // Android HOME must remain active when Back is pressed on the dock.
        // The v7 preview continues to exit to Google TV as before.
        if (isStandalonePreview()) finish();
    }
}
