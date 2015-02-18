package com.forizon.jimage.viewer.context;

import com.forizon.jimage.viewer.actions.AbstractAction;
import com.forizon.jimage.viewer.actions.AbstractToggleableAction;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

class MenuBarBuilder {
    final JMenuBar bar;
    final SortedMap<String, AbstractAction> actions;
    final Context context;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public MenuBarBuilder(Context context) {
        this.context = context;
        bar = new JMenuBar();
        // Make sure the entries are sorted
        actions = (context.getActions() instanceof SortedMap)
                ? (SortedMap)context.getActions()
                : new TreeMap<String, AbstractAction>(context.getActions());
    }

    public JMenuBar build() {
        Iterator<Map.Entry<String, AbstractAction>> iterator = actions.entrySet().iterator();
        Map.Entry<String, AbstractAction> entry;
        String currentGroup = null, currentMenu = null, group, menu;
        JMenu jMenu = null;
        AbstractAction action;
        while (iterator.hasNext()) {
            entry = iterator.next();
            // Determine menu
            menu = getMenu(entry.getKey());
            if (currentMenu == null || !menu.equals(currentMenu)) {
                jMenu = new JMenu(context.getLanguageSupport().localizeString("com.forizon.jimage.viewer.view.MenuBar." + menu + ".text"));
                bar.add(jMenu);
                currentMenu = menu;
                currentGroup = null;
            }
            // Determine group
            group = getGroup(entry.getKey());
            if (currentGroup != null && !group.equals(currentGroup)) {
                jMenu.addSeparator();
            }
            currentGroup = group;
            // Add item
            action = entry.getValue();
            if (action instanceof AbstractToggleableAction) {
                jMenu.add(new JCheckBoxMenuItem(action));
            } else {
                jMenu.add(action);
            }
        }
        return bar;
    }

    String getMenu(String string) {
        int pos = string.lastIndexOf('.');
        return (pos == -1)? string : string.substring(0, pos);
    }

    String getGroup(String string) {
        int pos = string.lastIndexOf('-');
        return (pos == -1)? getMenu(string) : string.substring(0, pos);
    }
}

