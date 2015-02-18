package org.javenstudio.mail.store;

import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class ImapUtility {
	private static Logger LOG = Logger.getLogger(ImapUtility.class); 

	/**
     * Gets all of the values in a sequence set per RFC 3501. Any ranges are expanded into a
     * list of individual numbers. If the set is invalid, an empty array is returned.
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range  = sequence-number ":" sequence-number
     * sequence-set    = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     */
    public static String[] getSequenceValues(String set) {
        ArrayList<String> list = new ArrayList<String>();
        if (set != null) {
            String[] setItems = set.split(",");
            for (String item : setItems) {
            	item = StringUtils.trim(item);
                if (item.indexOf(':') == -1) {
                    // simple item
                    try {
                        Integer.parseInt(item); // Don't need the value; just ensure it's valid
                        list.add(item);
                    } catch (NumberFormatException e) {
                        LOG.debug("Invalid UID value", e);
                    }
                } else {
                    // range
                    for (String rangeItem : getRangeValues(item)) {
                        list.add(rangeItem);
                    }
                }
            }
        }
        String[] stringList = new String[list.size()];
        return list.toArray(stringList);
    }
    
    /**
     * Expand the given number range into a list of individual numbers. If the range is not valid,
     * an empty array is returned.
     * <pre>
     * sequence-number = nz-number / "*"
     * sequence-range  = sequence-number ":" sequence-number
     * sequence-set    = (sequence-number / sequence-range) *("," sequence-set)
     * </pre>
     */
    public static String[] getRangeValues(String range) {
        ArrayList<String> list = new ArrayList<String>();
        try {
            if (range != null) {
                int colonPos = range.indexOf(':');
                if (colonPos > 0) {
                    int first  = Integer.parseInt(range.substring(0, colonPos));
                    int second = Integer.parseInt(range.substring(colonPos + 1));
                    if (first < second) {
                        for (int i = first; i <= second; i++) {
                            list.add(Integer.toString(i));
                        }
                    } else {
                        for (int i = first; i >= second; i--) {
                            list.add(Integer.toString(i));
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            LOG.debug("Invalid range value", e);
        }
        String[] stringList = new String[list.size()];
        return list.toArray(stringList);
    }
	
}
