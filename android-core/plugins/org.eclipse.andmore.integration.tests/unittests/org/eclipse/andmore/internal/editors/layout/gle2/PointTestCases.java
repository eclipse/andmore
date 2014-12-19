/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.editors.layout.gle2;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

import junit.framework.TestCase;

/**
 * Common utilities for the point tests {@link LayoutPointTest} and
 * {@link ControlPointTest}
 */
public class PointTestCases extends TestCase {
    LayoutCanvas mCanvas = new TestLayoutCanvas();

    protected MouseEvent canvasMouseEvent(int x, int y, int stateMask) {
        Event event = new Event();
        event.x = x;
        event.y = y;
        event.stateMask = stateMask;
        event.widget = mCanvas;
        MouseEvent mouseEvent = new MouseEvent(event);
        return mouseEvent;
    }

    /** Mock implementation of LayoutCanvas */
    protected static class TestLayoutCanvas extends LayoutCanvas {
        float mScaleX;

        float mScaleY;

        float mTranslateX;

        float mTranslateY;

        public TestLayoutCanvas(float scaleX, float scaleY, float translateX, float translateY) {
            super(null, null, new Shell(), 0);

            this.mScaleX = scaleX;
            this.mScaleY = scaleY;
            this.mTranslateX = translateX;
            this.mTranslateY = translateY;
        }

        public TestLayoutCanvas() {
            this(2.0f, 2.0f, 20.0f, 20.0f);
        }

        @Override
        CanvasTransform getHorizontalTransform() {
            ScrollBar scrollBar = new List(this, SWT.V_SCROLL|SWT.H_SCROLL).getHorizontalBar();
            return new TestCanvasTransform(scrollBar, mScaleX, mTranslateX);
        }

        @Override
        CanvasTransform getVerticalTransform() {
            ScrollBar scrollBar = new List(this, SWT.V_SCROLL|SWT.H_SCROLL).getVerticalBar();
            return new TestCanvasTransform(scrollBar, mScaleY, mTranslateY);
        }
    }

    static class TestCanvasTransform extends CanvasTransform {
        float mScale;

        float mTranslate;

        public TestCanvasTransform(ScrollBar scrollBar, float scale, float translate) {
            super(null, scrollBar);
            this.mScale = scale;
            this.mTranslate = translate;
        }

        @Override
        public int translate(int value) {
            return (int) ((value - mTranslate) / mScale);
        }

        @Override
        public int inverseTranslate(int value) {
            return (int) (value * mScale + mTranslate);
        }
    }

    public void testDummy() {
        // To avoid JUnit warning that this class contains no tests, even though
        // this is an abstract class and JUnit shouldn't try
    }
}
