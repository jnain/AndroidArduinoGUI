package org.sintef.jarduino;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Jonathan
 * Date: 29/08/13
 * Time: 12:11
 */
public class LogAdapter extends ArrayAdapter<LogItem> {
    public LogAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public LogAdapter(Context context, int textViewResourceId, LogItem[] objects) {
        super(context, textViewResourceId, objects);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId, LogItem[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public LogAdapter(Context context, int textViewResourceId, List<LogItem> objects) {
        super(context, textViewResourceId, objects);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId, List<LogItem> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public void add (String str, LogObject object){
        add(new LogItem(str, object));
    }
}
