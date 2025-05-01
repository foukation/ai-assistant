package com.skythinker.gptassistant.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AppStateTracker {
    public static final int STATE_FOREGROUND = 0;

    public static final int STATE_BACKGROUND = 1;

    private static int currentState;

    public static int getCurrentState() {
        return currentState;
    }

    public interface AppStateChangeListener {
        void appTurnIntoForeground();
        void appTurnIntoBackGround();
    }

    public static void track(Application application, final AppStateChangeListener appStateChangeListener){

        application.registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks(){

            private int resumeActivityCount = 0;

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (resumeActivityCount==0){
                    currentState = STATE_FOREGROUND;
                    appStateChangeListener.appTurnIntoForeground();
                }

                resumeActivityCount++;
            }


            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                resumeActivityCount--;

                if (resumeActivityCount==0){
                    currentState = STATE_BACKGROUND;
                    appStateChangeListener.appTurnIntoBackGround();
                }

            }
        });
    }

    private static class SimpleActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks{


        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }
    }
}
