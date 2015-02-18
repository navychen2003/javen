package com.forizon.jimage.viewer.util;

import com.forizon.jimage.viewer.context.Context;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.javenstudio.common.util.Strings;
import org.javenstudio.raptor.util.StringUtils;

public class DefaultLanguageSupport implements LanguageSupport {
    final ResourceBundle resourceBundle;
    final Context context;

    public DefaultLanguageSupport(Context context, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.context = context;
    }

    @Override
    public Object localizeObject(String key) {
        Object result = null;
        try {
            result = resourceBundle.getObject(key);
        } catch (MissingResourceException e) {
            context.getReporter().report(e);
        }
        return result;
    }

    @Override
    public String localizeString(String key) {
        String result = null;
        try {
            result = resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            context.getReporter().report(e);
        }
        return Strings.get(StringUtils.trim(result));
    }
}
