/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kpz.pomodorotasks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class TouchInterceptor extends ListView {
    
    private static final String TAG = "PomodoroTasks";
	private ImageView mDragView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private int mDragPos;      // which item is being dragged
    private int mFirstDragPos; // where was the dragged item originally
    private int mDragPoint;    // at what offset inside the item did the user grab it
    private int mCoordOffset;  // the difference between screen coordinates and coordinates in this view
    private DragListener mDragListener;
    private DropListener mDropListener;
    private CheckOffListener mCheckOffListener;
    private int mUpperBound;
    private int mLowerBound;
    private int mHeight;
    private GestureDetector mGestureDetector;
    private static final int FLING = 0;
    private static final int SLIDE = 1;
    private int mCheckOffMode = -1;
    private Rect mTempRect = new Rect();
    private Bitmap mDragBitmap;
    private final int mTouchSlop;
    private int mItemHeightNormal;
    private int mItemHeightExpanded;
	private Context mContext;
	
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 100; // 200 

    public TouchInterceptor(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mContext = context;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        Resources res = getResources();
        mItemHeightNormal = res.getDimensionPixelSize(R.dimen.normal_height);
        mItemHeightExpanded = res.getDimensionPixelSize(R.dimen.expanded_height);
        mCheckOffMode = FLING;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	
        if (mCheckOffListener != null && mGestureDetector == null) {
            if (mCheckOffMode == FLING) {
            	
                mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                            float velocityY) {

                    	int itemnum = pointToPosition((int)e1.getX(), (int)e1.getY());
                    	Log.d(TAG, "touch interceptor onFling itemnum: " + itemnum + " ; velocityX:" + velocityX + " ; ev2.getX():" + e2.getX() + " ; e1.getX():" + e1.getX());

						if (itemnum != INVALID_POSITION && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

	                    	if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){
						    	
						    	return false;
						    }

							if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {

								Log.d(TAG, "left to right");
								stopDragging();
								mCheckOffListener.checkOff(itemnum);
									unExpandViews(false);

								return true;

							} else if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {

								Log.d(TAG, "right to left");
								stopDragging();
								mCheckOffListener.uncheckOff(itemnum);
									unExpandViews(false);

								return true;
							}
						}
                        return false;
                    }
                });
            }
        }
        if (mDragListener != null || mDropListener != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    int itemnum = pointToPosition(x, y);
                    if (itemnum == AdapterView.INVALID_POSITION) {
                        break;
                    }
                    ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
                    mDragPoint = y - item.getTop();
                    mCoordOffset = ((int)ev.getRawY()) - y;
                    View dragger = item.findViewById(R.id.icon);

                    Rect r = mTempRect;
                    dragger.getDrawingRect(r);
                    // The dragger icon itself is quite small, so pretend the touch area is bigger
                    if (x < r.right) {
                        item.setDrawingCacheEnabled(true);
                        // Create a copy of the drawing cache so that it does not get recycled
                        // by the framework when the list tries to clean up memory
                        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                        startDragging(bitmap, y);
                        mDragPos = itemnum;
                        mFirstDragPos = mDragPos;
                        mHeight = getHeight();
                        int touchSlop = mTouchSlop;
                        mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                        mLowerBound = Math.max(y + touchSlop, mHeight * 2 /3);
                        return false;
                    }
                    mDragView = null;
                    break;
            }
        }
        
        return super.onInterceptTouchEvent(ev);
    }
    
    /*
     * pointToPosition() doesn't consider invisible views, but we
     * need to, so implement a slightly different version.
     */
    private int myPointToPosition(int x, int y) {
        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }
    
    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPoint - 32;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            pos = 0;
        }
        return pos;
    }
    
    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    /*
     * Restore size and visibility for all listitems
     */
    private void unExpandViews(boolean deletion) {
        for (int i = 0;; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    // HACK force update of mItemCount
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                    // end hack
                }
                layoutChildren(); // force children to be recreated where needed
                v = getChildAt(i);
                if (v == null) {
                    break;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }
    
    /* Adjust visibility and size to make it appear as though
     * an item is being dragged around and other items are making
     * room for it:
     * If dropping the item would result in it still being in the
     * same place, then make the dragged listitem's size normal,
     * but make the item invisible.
     * Otherwise, if the dragged listitem is still on screen, make
     * it as small as possible and expand the item below the insert
     * point.
     * If the dragged item is not on screen, only expand the item
     * below the current insertpoint.
     */
    private void doExpansion() {
        int itemRightBelowDragPos = mDragPos - getFirstVisiblePosition();
        int total = getCount();
        if (mDragPos > mFirstDragPos && mDragPos != total - 1) {
            itemRightBelowDragPos++;
        }
		Log.d(TAG, "begin mDragPos:" + mDragPos + " total:" + total + " getFirstVisiblePosition:" + getFirstVisiblePosition() + " rightBelow:" + itemRightBelowDragPos);

        View itemBeingDragged = getChildAt(mFirstDragPos - getFirstVisiblePosition());
        for (int currentNodePos = 0;; currentNodePos++) {

        	Log.d(TAG, "currentNodePos:" + currentNodePos);  

        	int gravity = Gravity.BOTTOM;
            View currentNode = getChildAt(currentNodePos);
            if (currentNode == null) {
            	Log.d(TAG, "break total:" + currentNodePos); 
                break;
            }
            int height = mItemHeightNormal;
            int visibility = View.VISIBLE;
            
            if (currentNode.equals(itemBeingDragged)) {
                // processing the item that is being dragged
                if (mDragPos == mFirstDragPos) {
                    // hovering over the original location
                     visibility = View.INVISIBLE;
                } else {
                    // not hovering over it
                    height = 1;
                }
            } else if (currentNodePos == itemRightBelowDragPos) {

        		Log.d(TAG, "before condition i:" + currentNodePos + " total:" + total + " last:" + getLastVisiblePosition());
        		
        		if (mDragPos == total - 1){	
        		
            		Log.d(TAG, "in condition");
            		height = mItemHeightExpanded;
            		gravity = Gravity.TOP;
					
            	} else {
            	
	                if (mDragPos < getCount() - 1) {
	                    height = mItemHeightExpanded;
	                }
            	}
            }	
            
            ViewGroup.LayoutParams params = currentNode.getLayoutParams();
            params.height = height;
            currentNode.setLayoutParams(params);
            currentNode.setVisibility(visibility);
            
            LinearLayout layout = (LinearLayout)currentNode.findViewById(R.id.taskRow);
            layout.setGravity(gravity);
        }
    }

	private void makeRoomAtListBottom(View vv) {
		LinearLayout.LayoutParams viewGroupParams = (LinearLayout.LayoutParams)getLayoutParams();
		viewGroupParams.bottomMargin = vv.getHeight();
		setLayoutParams(viewGroupParams);
	}
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	
        if (mGestureDetector != null) {
            if(mGestureDetector.onTouchEvent(ev)){
            	return true;
            }
        }
        
        if ((mDragListener != null || mDropListener != null) && mDragView != null) {
            int action = ev.getAction(); 
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = mTempRect;
                    mDragView.getDrawingRect(r);
                    stopDragging();
                
                    float expectedRightValue = r.right * 1/2;
                    if (mCheckOffMode == SLIDE && ev.getX() > expectedRightValue) {
                    	if (mCheckOffListener != null) {
                    		// Not implemented
                            //mRemoveListener.checkOff(mFirstDragPos);
                        }
                        unExpandViews(true);
                    } else {
                        if (mDropListener != null && mDragPos >= 0 && mDragPos < getCount()) {
                            mDropListener.drop(mFirstDragPos, mDragPos);
                        }
                        unExpandViews(false);
                    }
                    break;
                    
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
                            if (mDragListener != null) {
                                mDragListener.drag(mDragPos, itemnum);
                            }
                            mDragPos = itemnum;
                            
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > mLowerBound) {
                            // scroll the list up a bit
                            speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                        } else if (y < mUpperBound) {
                            // scroll the list down a bit
                            speed = y < mUpperBound / 2 ? -16 : -4;
                        }
                        if (speed != 0) {
                            int ref = pointToPosition(0, mHeight / 2);
                            if (ref == AdapterView.INVALID_POSITION) {
                                //we hit a divider or an invisible view, check somewhere else
                                ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
                            }
                            View v = getChildAt(ref - getFirstVisiblePosition());
                            if (v!= null) {
                                int pos = v.getTop();
                                setSelectionFromTop(ref, pos - speed);
                            }
                        }
                    }
                    break;
            }
            return true;
        }
        
        return super.onTouchEvent(ev);
    }
    
    private void startDragging(Bitmap bm, int y) {
        stopDragging();

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPoint + mCoordOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        
        ImageView v = new ImageView(mContext);
        int backGroundColor = mContext.getResources().getColor(R.color.dragndrop_background);
        v.setBackgroundColor(backGroundColor);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager)mContext.getSystemService("window");
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }
    
    private void dragView(int x, int y) {
        if (mCheckOffMode == SLIDE) {
            float alpha = 1.0f;
            int width = mDragView.getWidth();
            if (x > width / 2) {
                alpha = ((float)(width - x)) / (width / 2);
            }
            mWindowParams.alpha = alpha;
        }
        mWindowParams.y = y - mDragPoint + mCoordOffset;
        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }
    
    private void stopDragging() {
        if (mDragView != null) {
            WindowManager wm = (WindowManager)mContext.getSystemService("window");
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
        if (mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }
    }
    
    public void setDragListener(DragListener l) {
        mDragListener = l;
    }
    
    public void setDropListener(DropListener l) {
        mDropListener = l;
    }
    
    public void setCheckOffListener(CheckOffListener l) {
        mCheckOffListener = l;
    }

    public interface DragListener {
        void drag(int from, int to);
    }
    public interface DropListener {
        void drop(int from, int to);
    }
    public interface CheckOffListener {
        void checkOff(int which);

		void uncheckOff(int mFirstDragPos);
    }
}
