/*
 * Copyright (C) 2012 Soomla Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.soomla.social.example;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;

import java.util.Arrays;
import java.util.List;

/**
 * Created by oriargov on 5/14/14.
 */
public class FacebookEnabledFragment extends Fragment {
    private static final String TAG = "FacebookEnabledFragment";

    protected UiLifecycleHelper uiHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), statusCallback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        uiHelper.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e(TAG, String.format("Error: %s", error.toString()));
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i(TAG, "uiHelper.onActivityResult.onComplete");
                if (FacebookDialog.getNativeDialogDidComplete(data)) {
                    if (FacebookDialog.getNativeDialogCompletionGesture(data) == null
                            || FacebookDialog.COMPLETION_GESTURE_CANCEL.equals(FacebookDialog.getNativeDialogCompletionGesture(data))) {
                        // track cancel
                        Log.d(TAG, "FB dialog cancelled");
                    } else {
                        // track post
                        String postId = FacebookDialog.getNativeDialogPostId(data);
                        Log.d(TAG, "postId = " + postId);
                    }
                } else {
                    // track cancel
                    Log.d(TAG, "FB dialog cancelled");
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    protected void onLogin() {}
    protected void onLogout() {}

    protected void publishFeedDialog() {
        Bundle params = new Bundle();
        params.putString("name", "Facebook SDK for Android");
        params.putString("caption", "Build great social apps and get more installs.");
        params.putString("description", "The Facebook SDK for Android makes it easier and faster to develop Facebook integrated Android apps.");
        params.putString("link", "https://developers.facebook.com/android");
        params.putString("picture", "https://raw.github.com/fbsamples/ios-3.x-howtos/master/Images/iossdk_logo.png");

        WebDialog feedDialog = (
                new WebDialog.FeedDialogBuilder(getActivity(),
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error == null) {
                            // When the story is posted, echo the success
                            // and the post Id.
                            final String postId = values.getString("post_id");
                            if (postId != null) {
                                Toast.makeText(getActivity(),
                                        "Posted story, id: " + postId,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // User clicked the Cancel button
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Publish cancelled",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else if (error instanceof FacebookOperationCanceledException) {
                            // User clicked the "x" button
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "Publish cancelled",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Generic, ex: network error
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "Error posting story",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                })
                .build();
        feedDialog.show();
    }

    private void onClickLogin() {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(new Session.OpenRequest(this)
                    .setPermissions(Arrays.asList("public_profile"))
                    .setCallback(statusCallback));
        } else {
            Session.openActiveSession(getActivity(), this, true, statusCallback);
        }
    }

    private void checkPermissions() {
        final List<String> permissions = Session.getActiveSession().getPermissions();
        final List<String> declinedPermissions = Session.getActiveSession().getDeclinedPermissions();
    }

    private void requestCheckInPermission(Session session) {
        Session.NewPermissionsRequest newPermissionsRequest = new Session
                .NewPermissionsRequest(this, Arrays.asList("user_checkins"));
        session.requestNewReadPermissions(newPermissionsRequest);
    }

    private void requestPublishPermission(Session session) {
        Session.NewPermissionsRequest newPermissionsRequest = new Session
                .NewPermissionsRequest(this, Arrays.asList("publish_actions"));
        session.requestNewPublishPermissions(newPermissionsRequest);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            Log.i(TAG, "Logged in...");
            onLogin();
        } else if (state.isClosed()) {
            Log.i(TAG, "Logged out...");
            onLogout();
        }
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
}
