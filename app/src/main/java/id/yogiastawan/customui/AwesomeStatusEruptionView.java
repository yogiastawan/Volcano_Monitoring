package id.yogiastawan.customui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import id.michale.volcanomonitoring.R;

/**
 * TODO: document your custom view class.
 */
public class AwesomeStatusEruptionView extends View {
    private String mLabel; // TODO: use a default from R.string...
    private Drawable mValue; // TODO: use a default from R.color...
    private String mStatus; // TODO: use a default from R.dimen...

//    private String mValueUnit;

    private float mSecondarySize=0;
    private float mImageSize =0;

    private TextPaint mLabelPaint;
//    private TextPaint mValuePaint;
    private TextPaint mStatusPaint;

    private Paint bgPaint=new Paint();
    private Paint bgStrokePaint=new Paint();

    private float mLabelWidth;
    private float mLabelHeight;
    private float mStatusWidth;
    private float mStatusHeight;

    Rect bounds=new Rect();

    Path bgPath=new Path();

    public AwesomeStatusEruptionView(Context context) {
        super(context);
        init(null, 0);
    }

    public AwesomeStatusEruptionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public AwesomeStatusEruptionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.AwesomeStatusEruptionView, defStyle, 0);

        mLabel = a.getString(
                R.styleable.AwesomeStatusEruptionView_labelname);
        if (a.hasValue(R.styleable.AwesomeStatusEruptionView_image)) {
            mValue = a.getDrawable(
                    R.styleable.AwesomeStatusEruptionView_image);
            mValue.setCallback(this);
        }
        mStatus = a.getString(
                R.styleable.AwesomeStatusEruptionView_statuseruption);

        mSecondarySize=a.getDimension(R.styleable.AwesomeStatusEruptionView_textsize,mSecondarySize);
        mImageSize =a.getDimension(R.styleable.AwesomeStatusEruptionView_imagesize, mImageSize);



        a.recycle();

        // Set up a default TextPaint object
        mLabelPaint = new TextPaint();
        mLabelPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLabelPaint.setTextAlign(Paint.Align.LEFT);


        mStatusPaint = new TextPaint();
        mStatusPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mStatusPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

        mLabelPaint.setColor(Color.BLACK);
        mStatusPaint.setColor(Color.BLACK);

        mLabelPaint.setTextSize(mSecondarySize);
        Paint tPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        if (mLabel!=null) {
            tPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);

            mLabelWidth = bounds.width();
            mLabelHeight = bounds.height();
        }
        // imagePaint

        mStatusPaint.setTextSize(mSecondarySize);
//        mStatusWidth= mStatusPaint.measureText(mStatus);
//        fontMetrics = mStatusPaint.getFontMetrics();
//        mStatusHeight = fontMetrics.bottom-fontMetrics.top+fontMetrics.leading;
        if (mStatus!=null) {
            tPaint.getTextBounds(mStatus, 0, mStatus.length(), bounds);
            mStatusWidth = bounds.width();
            mStatusHeight = bounds.height();
        }

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

        int imageSize=0;
        if (contentHeight<=contentWidth){
            imageSize=contentHeight-75;
        }else if (contentHeight>contentWidth){
            imageSize=contentWidth-75;
        }

        if (imageSize<=0) imageSize=0;


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

               // Draw the image.
        if (mValue!=null) {
            mValue.setBounds(paddingLeft+((contentWidth-imageSize)/2), paddingTop+((contentHeight-imageSize)/2), paddingLeft+((contentWidth+imageSize)/2), paddingTop+((contentHeight+imageSize)/2));
            mValue.draw(canvas);
        }

        // Draw the text label.
        if (mLabel!=null) {
            canvas.drawText(mLabel,
                    (contentWidth - mLabelWidth) / 2,
                    paddingTop + mLabelHeight,
                    mLabelPaint);
        }

        // Draw the text status.
        if (mStatus!=null) {
            canvas.drawText(mStatus,
                    (contentWidth - mStatusWidth) / 2,
                    paddingTop + (contentHeight),
                    mStatusPaint);
        }

    }


    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }


    public Drawable getValue() {
        return mValue;
    }


    public void setValue(Drawable value) {
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
        return mImageSize;
    }

    public void setPrimarySize(float primarySize){
        mImageSize =primarySize;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

}
