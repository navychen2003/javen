package com.forizon.jimage.viewer.actions;

import com.forizon.jimage.viewer.context.Context;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Builds all of the <code>{@link javax.swing.Action}</code>s used by
 * JImageView.
 */
public class ActionsBuilder {
    // MenuBar constants these are in the form menu.submenu.group.Action
    // which is converted into a menu hirearchy to create a JMenuBar
    // these constants are also used as keys for Actions
    // see JavaDoc for getActions()
    public static final String FILE_MENU = "File";
    public static final String FILE_PRINT_GROUP = FILE_MENU + ".Print";
    public static final String FILE_DELETE_GROUP = FILE_MENU + ".Delete";
    public static final String FILE_DOCUMENT_GROUP = FILE_MENU + ".1Document";
    public static final String FILE_NAVIGATION_GROUP = FILE_MENU + ".Navigation";
    public static final String FILE_WINDOW_GROUP = FILE_MENU + ".Window";
    public static final String VIEW_MENU = "View";
    public static final String VIEW_ZOOM_GROUP = VIEW_MENU + ".Zoom";
    public static final String VIEW_ROTATE_GROUP = VIEW_MENU + ".Rotate";
    public static final String VIEW_FLIP_GROUP = VIEW_MENU + ".Flip";
    public static final String VIEW_MODE_GROUP = VIEW_MENU + ".Mode";
    public static final String WINDOW_MENU = "Window";
    public static final String WINDOW_ALPHA_GROUP = WINDOW_MENU + ".Alpha";
    public static final String WINDOW_OPTIONS_GROUP = WINDOW_MENU + ".Options";
    public static final String HELP_MENU = "Help";
    public static final String HELP_ALPHA_GROUP = HELP_MENU + ".Alpha";
    public static final String HELP_MENU_ONLINE_GROUP = HELP_MENU + ".Online";
    /** Current context */
    private final Context context;
    /** Actions are maped by their keys */
    TreeMap<String, AbstractAction> actions;

    public ActionsBuilder(Context aContext) {
        context = aContext;
    }

    /**
     * Returns a map of <code>Action</code>s. <code>Action</code>s are mapped
     * using a <code>{@link java.util.SortedMap}</code> to a <code>String</code>
     * key which also serves to represent the menu's hirearchy used to create a
     * JMenuBar.
     * 
     * A Key has the form menu[.submenu.subsubmenu.etc].group-Action. The last
     * fragment (Action) represents the name of the item. All items with the
     * same remaining fragments including are grouped together and are
     * represented by all fragments up to and including the second last key
     * fragment (group). Menus are represented by the remaining key fragments up
     * to and including the third last and are nested to match the hirearchy
     * represented by the constituent fragments. All groups with the same
     * remaining fragments are added to the same menu and are deliminated with
     * seperators.
     * 
     * Keys are also used to localize the generated menu and menu items.
     * 
     * @return a map of <code>Action</code>s
     */
    public SortedMap<String, AbstractAction> getActions() {
        if (actions == null) {
            actions = new TreeMap<String, AbstractAction>();
            buildActionMap();
        }
        return actions;
    }

    /** Builds all actions used by JImageView. */
    protected void buildActionMap() {
        // There should be a better place to put this..
        Print printAction = new Print(context);

        actions.put(FILE_DOCUMENT_GROUP + "-Open", new Open(context));
        actions.put(FILE_DOCUMENT_GROUP + "-OpenURI", new OpenURI(context));
        actions.put(FILE_DOCUMENT_GROUP + "-SaveAs", new SaveAs(context));
        actions.put(FILE_DELETE_GROUP + "-Delete", new Delete(context));
        actions.put(FILE_PRINT_GROUP + "-PageSetup", new PageSetup(context, printAction));
        actions.put(FILE_PRINT_GROUP + "-Print", printAction);
        actions.put(FILE_NAVIGATION_GROUP + "-Next", new Next(context));
        actions.put(FILE_NAVIGATION_GROUP + "-Previous", new Previous(context));
        actions.put(FILE_WINDOW_GROUP + "-Close", new Close(context));
        actions.put(VIEW_ZOOM_GROUP + "-ZoomIn", new ZoomIn(context));
        actions.put(VIEW_ZOOM_GROUP + "-ZoomOut", new ZoomOut(context));
        actions.put(VIEW_ZOOM_GROUP + "-ZoomBest", new ZoomBest(context));
        actions.put(VIEW_ZOOM_GROUP + "-ZoomNormal", new ZoomNormal(context));
        actions.put(VIEW_ROTATE_GROUP + "-RotateRight", new RotateRight(context));
        actions.put(VIEW_ROTATE_GROUP + "-RotateLeft", new RotateLeft(context));
        actions.put(VIEW_ROTATE_GROUP + "-RotateNormal", new RotateNormal(context));
        actions.put(VIEW_FLIP_GROUP + "-FlipHorizontally", new FlipHorizontally(context));
        actions.put(VIEW_FLIP_GROUP + "-FlipVertically", new FlipVertically(context));
        actions.put(VIEW_MODE_GROUP + "-FullScreen", new FullScreen(context));
        actions.put(VIEW_MODE_GROUP + "-SlideShow", new SlideShow(context));
        actions.put(WINDOW_ALPHA_GROUP + "-NewWindow", new NewWindow(context));
        actions.put(WINDOW_OPTIONS_GROUP + "-Options", new Options(context));
        //actions.put(HELP_ALPHA_GROUP + "-About", new About(context));
        //actions.put(HELP_MENU_ONLINE_GROUP + "-OpenHomepage", new OpenHomepage(context));
        //actions.put(HELP_MENU_ONLINE_GROUP + "-OpenForums", new OpenSupport(context));
    }
}

