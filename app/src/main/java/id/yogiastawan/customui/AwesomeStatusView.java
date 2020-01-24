package id.yogiastawan.customui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import id.michale.volcanomonitoring.R;

/**
 * TODO: document your custom view class.
 */
public class AwesomeStatusView extends View {
    private String mLabel; // TODO: use a default from R.string...
    private String mValue; // TODO: use a default from R.color...
    private String mStatus; // TODO: use a default from R.dimen...
    private String mUnit;

//    private String mValueUnit;

    private float mSecondarySize=0;
    private float mPrimarySize=0;

    private TextPaint mLabelPaint;
    private TextPaint mValuePaint;
    private TextPaint mStatusPaint;

    private Paint bgPaint=new Paint();
    private Paint bgStrokePaint=new Paint();

    private float mLabelWidth;
    private float mLabelHeight;
    private float mValueWidth;
    private float mValueHeight;
    private float mStatusWidth;
    private float mStatusHeight;

    Rect bounds=new Rect();

    Path bgPath=new Path();

    public AwesomeStatusView(Context context) {
        super(context);
        init(null, 0);
    }

    public AwesomeStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public AwesomeStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.AwesomeStatusView, defStyle, 0);

        mLabel = a.getString(
                R.styleable.AwesomeStatusView_label);
        mValue = a.getString(R.styleable.AwesomeStatusView_value);
        mStatus = a.getString(
                R.styleable.AwesomeStatusView_status);

        mSecondarySize=a.getDimension(R.styleable.AwesomeStatusView_secondarySize,mSecondarySize);
        mPrimarySize=a.getDimension(R.styleable.AwesomeStatusView_primarySize,mPrimarySize);

        mUnit=a.getString(R.styleable.AwesomeStatusView_unit);


        a.recycle();

        // Set up a default TextPaint object
        mLabelPaint = new TextPaint();
        mLabelPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLabelPaint.setTextAlign(Paint.Align.LEFT);

        mValuePaint = new TextPaint();
        mValuePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mValuePaint.setTextAlign(Paint.Align.CENTER);

        mStatusPaint = new TextPaint();
        mStatusPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mStatusPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

        mLabelPaint.setColor(Color.BLACK);
        mValuePaint.setColor(Color.BLACK);
        mStatusPaint.setColor(Color.BLACK);

        mLabelPaint.setTextSize(mSecondarySize);
        Paint tPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        tPaint.getTextBounds(mLabel,0,mLabel.length(),bounds);
        mLabelWidth=bounds.width();
        mLabelHeight=bounds.height();

        if (mUnit==null){
            mUnit="";
        }

        mValuePaint.setTextSize(mPrimarySize);
//        mValueWidth = mValuePaint.measureText(mValue);
//        fontMetrics = mValuePaint.getFontMetrics();
//        mValueHeight = fontMetrics.bottom-fontMetrics.top+fontMetrics.leading;
        tPaint.getTextBounds(mValue+mUnit,0,(mValue+mUnit).length(),bounds);
        mValueWidth=bounds.width();
        mValueHeight=bounds.height();

        Log.d("wh", "width: "+mValueWidth);

        mStatusPaint.setTextSize(mSecondarySize);
//        mStatusWidth= mStatusPaint.measureText(mStatus);
//        fontMetrics = mStatusPaint.getFontMetrics();
//        mStatusHeight = fontMetrics.bottom-fontMetrics.top+fontMetrics.leading;
        tPaint.getTextBounds(mStatus,0,mStatus.length(),bounds);
        mStatusWidth=bounds.width();
        mStatusHeight=bounds.height();

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStrokeWidth(2);
        bgStrokePaint.setStyle(Paint.Style.FILL);
        bgStrokePaint.setColor(Color.argb(85,0xBE,0xBE,0xBE));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - (paddingLeft + paddingRight);
        int contentHeight = getHeight() - (paddingTop + paddingBottom);


        //draw background
        bgPath.reset();
        bgPath.moveTo(getWidth()/6,0);
        bgPath.lineTo(getWidth(),0);
        bgPath.lineTo(getWidth(),getHeight()-(getHeight()/6));
        bgPath.lineTo(getWidth()-(getWidth()/6),getHeight());
        bgPath.lineTo(0,getHeight());
        bgPath.lineTo(0,getHeight()/6);
        bgPath.close();

        canvas.drawPath(bgPath,bgPaint);
        canvas.drawPath(bgPath,bgStrokePaint);

        // Draw the text label.
        canvas.drawText(mLabel,
                (contentWidth-mLabelWidth)/2,
                paddingTop+mLabelHeight,
                mLabelPaint);

        // Draw the text value.
        canvas.drawText(mValue+mUnit,
                paddingLeft+(contentWidth+mValueWidth)/2,
                paddingTop + (contentHeight+mValueHeight)/2,
                mValuePaint);

        Log.d("wh", "cw: "+contentWidth + " | gw: "+getWidth());
        Log.d("wh", "onDraw: "+paddingLeft + (contentWidth - mValueWidth) / 2);

        // Draw the text status.
        canvas.drawText(mStatus,
                (contentWidth - mStatusWidth) / 2,
                paddingTop + (contentHeight),
                mStatusPaint);


    }


    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
        invalidateTextPaintAndMeasurements();
    }


    public String getValue() {
        return mValue;
    }


    public void setValue(String value) {
        mValue = value;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }


    public String getStatus() {
        return mStatus;
    }


    public void setStatus(String status) {
        mStatus = status;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public float getSecondarySize(){
        return mSecondarySize;
    }

    public void setSecondarySize(float secondarySize){
        mSecondarySize=secondarySize;
        invalidateTextPaintAndMeasurements();
        invalidate();

    }

    public float getPrimarySize(){
        return mPrimarySize;
    }

    public void setPrimarySize(float primarySize){
        mPrimarySize=primarySize;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public String getUnit() {
        return mUnit;
    }

    public void setUnit(String unit) {
        mUnit = unit;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }
}
