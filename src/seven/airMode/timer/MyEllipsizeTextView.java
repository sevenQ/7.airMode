package seven.airMode.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class MyEllipsizeTextView extends TextView {

	public MyEllipsizeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyEllipsizeTextView(Context context) {
		super(context);
	}

	public MyEllipsizeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isFocused() {
		return true;
	}
	
	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text, type);
	}

}
