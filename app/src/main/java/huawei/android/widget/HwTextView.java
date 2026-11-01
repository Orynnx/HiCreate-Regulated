package huawei.android.widget;

import android.content.Context;
import android.util.AttributeSet;

/** Standard Android implementation for the Huawei-only HwTextView tag. */
public class HwTextView extends android.widget.TextView {
    public HwTextView(Context context) { super(context); }
    public HwTextView(Context context, AttributeSet attrs) { super(context, attrs); }
    public HwTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
