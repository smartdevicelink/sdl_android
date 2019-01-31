package com.smartdevicelink.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * {@code Localization} is a class to create Resources objects for
 * a specific locale (language, region, script) if resources of a locale
 * other than the default locale is required.
 */
public class Localization {
    private Locale locale;
    private Context context;

    /**
     * Return the locale object used by the Resource object of this instance.
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Return the resource object created by this instance.
     */
    public Resources getResources() {
        return this.context.getResources();
    }

    /**
     * Creates an instance with a resource object using default locale.
     * @param context A context required to create a new resource object.
     */
    public Localization(@NonNull Context context) {
        this(context, Locale.getDefault());
    }

    /**
     * Creates an instance with a resource object using the specified language code.
     * @param context A context required to create a new resource object.
     * @param locale The locale identifier for the resource object.
     */
    public Localization(@NonNull Context context, @NonNull Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Resources resources = context.getResources();
            Configuration config = new Configuration(resources.getConfiguration());
            config.setLocale(locale);
            
            ContextWrapper wrapper = new ContextWrapper(context);
            this.context = wrapper.createConfigurationContext(config);
        } else {
            this.context = context.getApplicationContext();
        }
        this.locale = locale;
    }
}
