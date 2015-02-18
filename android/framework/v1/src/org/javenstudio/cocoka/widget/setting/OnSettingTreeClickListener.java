package org.javenstudio.cocoka.widget.setting;

/**
 * Interface definition for a callback to be invoked when a
 * {@link Setting} in the hierarchy rooted at this {@link SettingScreen} is
 * clicked.
 */
public interface OnSettingTreeClickListener {
    /**
     * Called when a setting in the tree rooted at this
     * {@link SettingScreen} has been clicked.
     * 
     * @param settingScreen The {@link SettingScreen} that the
     *        setting is located in.
     * @param setting The setting that was clicked.
     * @return Whether the click was handled.
     */
    boolean onSettingTreeClick(SettingScreen settingScreen, Setting setting);
}
