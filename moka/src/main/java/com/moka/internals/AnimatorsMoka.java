package com.moka.internals;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import static android.os.Build.VERSION.SDK_INT;
import static com.moka.EspressoMokaRunner.runOnMainSync;
import static com.moka.internals.Reflection.invokeStatic;
import static com.moka.internals.Reflection.setFieldValue;

public final class AnimatorsMoka {

    private AnimatorsMoka() {

    }

    public static void disableAnimators() {
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                //android disabled access to hidden methods starting API 28
                if (SDK_INT < 28) {
                    setFieldValue(null, ValueAnimator.class, "sDurationScale", 0.0f);
                    setFieldValue(getStaticAnimationHandler(), getAnimationsCallBacks(), new ArrayListThatDisablesAddedAnimators<ValueAnimator>());
                }
            }

            @NonNull
            private String getAnimationsCallBacks() {
                return SDK_INT < 24 ? "mAnimations" : "mAnimationCallbacks";
            }

            @Nonnull
            private Object getStaticAnimationHandler() {
                if (SDK_INT < 24) {
                    return invokeStatic("android.animation.ValueAnimator", "getOrCreateAnimationHandler");
                } else {
                    return invokeStatic("android.animation.AnimationHandler", "getInstance");
                }
            }
        });
    }

    private static class ArrayListThatDisablesAddedAnimators<T> extends ArrayList<T> {
        @Override
        public boolean add(final T object) {
            disableAnimation(object);
            return super.add(object);
        }

        @Override
        public void add(final int index, final T object) {
            disableAnimation(object);
            super.add(index, object);
        }

        @Override
        public boolean addAll(final Collection<? extends T> collection) {
            for (T t : collection) {
                disableAnimation(t);
            }
            return super.addAll(collection);
        }

        @Override
        public boolean addAll(final int index, final Collection<? extends T> collection) {
            for (T t : collection) {
                disableAnimation(t);
            }
            return super.addAll(index, collection);
        }

        @Override
        public T set(final int index, final T object) {
            disableAnimation(object);
            return super.set(index, object);
        }

        private void disableAnimation(final Object object) {
            if (object instanceof Animator) {
                ((Animator) object).setDuration(0);
            }
            if (object instanceof AnimatorSet) {
                disableAnimation(((AnimatorSet) object).getChildAnimations());
            } else if (object instanceof ValueAnimator) {
                ((ValueAnimator) object).setRepeatCount(0);
            }
        }
    }
}
