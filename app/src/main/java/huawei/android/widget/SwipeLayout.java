package huawei.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal standard-Android replacement for Huawei's SwipeLayout.
 *
 * Layout convention used by the app: child 0 is the action container that sits
 * behind the card, child 1 is the visible card. The card is translated left to
 * reveal the actions; releasing past one third keeps it open.
 */
public class SwipeLayout extends FrameLayout {
    public enum Status { Close, Open, Full, Limit, Middle, Viscous, CLOSE, OPEN, SWIPING, NONE, HALF_OPEN, ALL_OPEN, ALL_CLOSE, Swiping, None, HalfOpen }
    public enum SwipeMode { ACTION, LINKAGE, SWIPE, SLIDE, MOVE, NONE, DEFAULT, Swipe, Slide, Move, None, Default }
    public enum DragEdge { Left, Right, Top, Bottom, None, LEFT, RIGHT, TOP, BOTTOM, NONE }
    public enum ShowMode { LayDown, PullOut, Merge, LAY_DOWN, PULL_OUT, MERGE }
    public interface SwipeListener {
        void onStartOpen(SwipeLayout layout);
        void onOpen(SwipeLayout layout);
        void onClose(SwipeLayout layout);
        void onUpdate(SwipeLayout layout, int leftOffset, int topOffset);
        void onHandRelease(SwipeLayout layout, float xvel, float yvel);
        void onDragThenClose(SwipeLayout layout, float offset);
    }
    public interface OnDragListener { void onDrag(float offset, Status status, boolean isLeft); }

    private Status status = Status.Close;
    private DragEdge dragEdge = DragEdge.Right;
    private SwipeMode swipeMode = SwipeMode.NONE;
    private ShowMode showMode = ShowMode.LayDown;
    private boolean swipeEnabled = true;
    private View actions;
    private View surface;
    private final List<SwipeListener> listeners = new ArrayList<>();
    private final List<OnDragListener> dragListeners = new ArrayList<>();
    private int touchSlop;
    private float downX, downY, lastX;
    private boolean dragging;
    private int actionWidth;

    public SwipeLayout(Context c) { super(c); init(); }
    public SwipeLayout(Context c, AttributeSet a) { super(c, a); init(); }
    public SwipeLayout(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setClipChildren(true);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() >= 2) {
            actions = getChildAt(0);
            surface = getChildAt(1);
            // Keep the action layer measurable; it sits behind the card.
            if (actions != null) actions.setVisibility(VISIBLE);
            if (surface != null) surface.bringToFront();
        }
    }

    @Override protected void onMeasure(int w, int h) {
        super.onMeasure(w, h);
        if (actions != null) actionWidth = actions.getMeasuredWidth();
    }

    public void addDrag(DragEdge e, View v) { dragEdge = e; actions = v; if (v != null) v.setVisibility(VISIBLE); }
    public void clearDragEdge() { dragEdge = DragEdge.None; }
    public void addSwipeListener(SwipeListener l) { if (l != null && !listeners.contains(l)) listeners.add(l); }
    public void removeAllSwipeListener() { listeners.clear(); }
    public void addOnDragListener(OnDragListener l) { if (l != null) dragListeners.add(l); }
    public Status getOpenStatus() { return status; }
    public void setOpenStatus(Status s) { status = s; }
    public void setOpenStatus(Status s, boolean a) { status = s; }
    public SwipeMode getSwipeMode() { return swipeMode; }
    public void setSwipeMode(SwipeMode m) { swipeMode = m; }
    public ShowMode getShowMode() { return showMode; }
    public void setShowMode(ShowMode m) { showMode = m; }
    public DragEdge getDragEdge() { return dragEdge; }
    public int getDragDistance() { return actionWidth; }
    public int getActionModeThreshold() { return Math.max(1, actionWidth) / 3; }
    public View getCurrentBottomView() { return actions; }
    public View getSurfaceView() { return surface == null ? this : surface; }
    public void setSwipeEnabled(boolean b) { swipeEnabled = b; }
    public void setSwipeEnabled(boolean b, boolean a) { swipeEnabled = b; }
    public void setSwipeEnabled(boolean b, boolean a, boolean f) { swipeEnabled = b; }
    public boolean isSwipeEnabled() { return swipeEnabled; }
    public void setSwipeDirection(int d) { dragEdge = d < 0 ? DragEdge.Right : DragEdge.Left; }
    public int getSwipeDirection() { return dragEdge == DragEdge.Left ? -1 : 1; }
    public void setSwipeOffset(float o) { setSwipeOffset(o, false); }
    public void setSwipeOffset(float o, boolean a) { applyOffset(o); }
    public float getSwipeOffset() { return surface == null ? 0f : surface.getTranslationX(); }
    public void setOnSwipeListener(Object l) {}
    public void open() { open(true, false); }
    public void open(boolean animate) { open(animate, false); }
    public void open(boolean animate, boolean notify) {
        status = Status.Open;
        smoothTo(-actionWidth, animate);
        if (notify) for (SwipeListener l : listeners) l.onOpen(this);
    }
    public void close() { close(true, false); }
    public void close(boolean animate) { close(animate, false); }
    public void close(boolean animate, boolean notify) {
        status = Status.Close;
        smoothTo(0, animate);
        if (notify) for (SwipeListener l : listeners) l.onClose(this);
    }
    public void setSwipeItemClickListener(Object l) {}
    public void setShadowOffset(float o) {}
    public void setShadowRadius(float r) {}
    public void setSwipeView(View v) {}

    private void smoothTo(float offset, boolean animate) {
        if (surface == null) return;
        surface.animate().cancel();
        if (animate) surface.animate().translationX(offset).setDuration(180).start();
        else surface.setTranslationX(offset);
    }

    private void applyOffset(float offset) {
        float min = -Math.max(1, actionWidth);
        offset = Math.max(min, Math.min(0, offset));
        if (surface != null) surface.setTranslationX(offset);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (!swipeEnabled || e == null || actions == null || actionWidth <= 0) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = e.getY();
                dragging = false;
                return false;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (!dragging && Math.abs(dx) > touchSlop
                        && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    dragging = true;
                    lastX = e.getX();
                    return true;
                }
                return dragging;
            }
            default:
                return dragging;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!swipeEnabled) return super.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = e.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float delta = e.getX() - lastX;
                lastX = e.getX();
                applyOffset(getSwipeOffset() + delta);
                status = Status.Swiping;
                float ratio = Math.abs(getSwipeOffset()) / Math.max(1, actionWidth);
                for (OnDragListener l : dragListeners) {
                    l.onDrag(ratio, status, dragEdge == DragEdge.Left);
                }
                for (SwipeListener l : listeners) {
                    l.onUpdate(this, (int) getSwipeOffset(), 0);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float threshold = Math.max(1, actionWidth) / 3f;
                if (Math.abs(getSwipeOffset()) > threshold) {
                    open(true, true);
                } else {
                    close(true, true);
                }
                dragging = false;
                return true;
            }
            default:
                return true;
        }
    }
}
