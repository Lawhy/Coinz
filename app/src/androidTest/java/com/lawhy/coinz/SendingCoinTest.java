package com.lawhy.coinz;


import android.Manifest;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.filters.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.schibsted.spain.barista.interaction.PermissionGranter;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed;
import static com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SendingCoinTest {

    /** This test is for one of the core functionality: sending/messaging a coin.
     * In my design, collected coins can only be sent to a fiend, so this test also
     * checks if the pop-up friend list is properly displayed.
     * Furthermore, the received foreign coin is BOUND, i.e. cannot be re-sent to others.
     *
     * Test account: espresso@test.com ; 123456 (password)
     * Test account's friend: espresso2@test.com ; 123456 (password)
     *
     * Thanks to the Barista Library, test code is wrapped into more readable one, e.g. the sleep method for idling.
     * */

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void resetTestAccount() {

        // Reset everything for the test account to have enough gold, coins and friends.
        FirebaseFirestore firestoreTest = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build();
        firestoreTest.setFirestoreSettings(settings);

        // Set the download date to today so that the coins, banked number, exchange rates would not be overridden.
        firestoreTest.collection("pool").document("downloadDate").update("f7TNuaiVucU7dV8l20RWZ55zmz82", new MyUtils().getCurrentDate());

        // Set gold number and banked number
        HashMap<String, Object> goldMap = new HashMap<>();
        goldMap.put("goldNumber", 10000.0);
        goldMap.put("bankedNumber", 24); // 24 so that only one more coin can be banked
        firestoreTest.collection("gold").document("espresso@test.com").set(goldMap);

        // Set two local coins and one foreign coin (foreign coin is not influenced by bank limit)
        HashMap<String, Object> coinMap1 = new HashMap<>();
        HashMap<String, Object> localCoin1 = new HashMap<>();
        localCoin1.put("id", "fake");
        localCoin1.put("currency", "SHIL");
        localCoin1.put("value", 8.0);
        coinMap1.put("0", localCoin1);
        HashMap<String, Object> localCoin2 = new HashMap<>();
        localCoin2.put("id", "fake");
        localCoin2.put("currency", "DOLR");
        localCoin2.put("value", 8.0);
        coinMap1.put("1", localCoin2);
        firestoreTest.collection("coins").document("espresso@test.com").set(coinMap1);

        HashMap<String, Object> coinMap2 = new HashMap<>();
        HashMap<String, Object> foreignCoin = new HashMap<>();
        foreignCoin.put("id", "fake");
        foreignCoin.put("currency", "PENY");
        foreignCoin.put("value", 8.0);
        foreignCoin.put("from", "No BODY");
        coinMap2.put("0", foreignCoin);
        firestoreTest.collection("foreignCoins").document("espresso@test.com").set(coinMap2);

    }

    @Test
    public void sendingCoinTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        sleep(1000);

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.floatingActionButton_login),
                        childAtPosition(
                                allOf(withId(R.id.mainWindow),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        sleep(2000);

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.AutEmail),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.v7.widget.CardView")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("espresso@test.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.AutPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.cardView2),
                                        0),
                                0),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("123456"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.AutSignInBtn), withText("Sign in"),
                        childAtPosition(
                                allOf(withId(R.id.Aut_layout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        sleep(10000);

        // Use the extra library for run-time permission grant
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION);

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.fab_menu),
                        childAtPosition(
                                allOf(withId(R.id.menu_layout),
                                        childAtPosition(
                                                withClassName(is("android.support.constraint.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        ViewInteraction floatingActionButton3 = onView(
                allOf(withId(R.id.fab_myAccount),
                        childAtPosition(
                                allOf(withId(R.id.menu_layout),
                                        childAtPosition(
                                                withClassName(is("android.support.constraint.ConstraintLayout")),
                                                2)),
                                1),
                        isDisplayed()));
        floatingActionButton3.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        sleep(10000);

        // Assert the statistics before sending the coin.
        // Assert the statistics of local coins are displayed
        assertDisplayed(R.id.localSHIL, "1");
        assertDisplayed(R.id.valSHIL, "8.000");
        assertDisplayed(R.id.localDOLR, "1");
        assertDisplayed(R.id.valDOLR, "8.000");
        // Assert the statistics of foreign coins are displayed
        assertDisplayed(R.id.foreignPENY, "1");
        assertDisplayed(R.id.valPENY,"8.000");
        // Assert the gold number is displayed
        assertDisplayed(R.id.goldNumber, "10000.000");

        ViewInteraction button = onView(
                allOf(withText("Send"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.coinsList),
                                        0),
                                3)));
        button.perform(scrollTo(), click());

        sleep(500);

        ViewInteraction textView = onView(
                allOf(withText("espresso2@test.com"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sendCoinFriendList),
                                        0),
                                0)));
        textView.perform(scrollTo(), click());

        sleep(1000);

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("Send"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton2.perform(scrollTo(), click());

        sleep(2000);

        // Ensure statistics update after sending a local coin to a friend
        assertDisplayed(R.id.localSHIL, "0");
        assertDisplayed(R.id.valSHIL,"0.000");

        // Ensure there is no send options in the foreign coin list
        assertNotDisplayed(R.id.foreignCoinsList, "SEND");
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
