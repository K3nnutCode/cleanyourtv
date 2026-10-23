package com.gathin.tvhome;

/**
 * HOME-eligible form of the tested CleanYourTV v7 four-banner UI.
 *
 * It inherits the exact v7 dock, focus, app grid, and soft background glow.
 * The independent v7 preview still exits on Back; the selected system HOME
 * candidate instead returns from the app grid to the dock and stays open.
 *
 * This class is only enabled by the homeCandidate manifest/build property.
 * It contains no DreamService or screensaver registration.
 */
public final class HomeCandidateActivity extends HomePreviewActivity {
    @Override
    protected boolean isStandalonePreview() {
        return false;
    }
}
