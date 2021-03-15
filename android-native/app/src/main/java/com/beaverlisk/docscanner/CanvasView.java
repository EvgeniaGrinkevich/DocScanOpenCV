package com.beaverlisk.docscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class CanvasView extends View {

    private final ArrayList<ViewShape> shapes = new ArrayList<>();

    private int paddingLeft = -1;
    private int paddingTop = -1;
    private int paddingRight = -1;
    private int paddingBottom = -1;

    public CanvasView(Context context) {
        super(context);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();
    }

    public static class ViewShape {

        private final Shape mShape;
        private final Paint mPaint;
        private final Paint mBorder;

        public ViewShape(Shape shape, Paint paint) {
            mShape = shape;
            mPaint = paint;
            mBorder = null;
        }

        public ViewShape(Shape shape, Paint paint, Paint border) {
            mShape = shape;
            mPaint = paint;
            mBorder = border;
            mBorder.setStyle(Paint.Style.STROKE);
        }

        public void draw(Canvas canvas) {
            mShape.draw(canvas, mPaint);
            if (mBorder != null) {
                mShape.draw(canvas, mBorder);
            }
        }

        public Shape getShape() {
            return mShape;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;
        for (ViewShape s : shapes) {
            s.getShape().resize(contentWidth, contentHeight);
            s.draw(canvas);
        }
    }

    public ViewShape addShape(Shape shape, Paint paint) {
        ViewShape viewShape = new ViewShape(shape, paint);
        shapes.add(viewShape);
        return viewShape;
    }

    public ViewShape addShape(Shape shape, Paint paint, Paint border) {
        ViewShape viewShape = new ViewShape(shape, paint, border);
        shapes.add(viewShape);
        return viewShape;
    }

    public void removeShape(ViewShape shape) {
        shapes.remove(shape);
    }

    public void removeShape(int index) {
        shapes.remove(index);
    }

    public void clear() {
        shapes.clear();
    }

}
