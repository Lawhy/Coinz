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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
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
public class ExpiredCoinsTest {

    /** The test deals with the case when coins are expired (on every Monday),
     * so by setting the last download date as some day last week, the coins should
     * be automatically cleared, thus the account statistics should be empty.
     *
     * Test Account: espresso2@test.com ; 123456 (password).
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

        // Set the download date to last week so that the coins are expected to expire.
        LocalDate now = LocalDate.now(); // Get today's date
        LocalDate dayOfLastWeek = now.minusWeeks(1); // Move it last week today
        String downloadDate = dayOfLastWeek.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")); // Format it as a downloadDate
        // And thus the Account statistics should be empty.
        firestoreTest.collection("pool")
                .document("downloadDate")
                .update("AqQe3M2ZwvgPTIhwrk8YnbkGXdu2", downloadDate);

        // Set two local coins and one foreign coin, the expected behavior is the unused three coins are expired.
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
        firestoreTest.collection("coins").document("espresso2@test.com").set(coinMap1);

        HashMap<String, Object> coinMap2 = new HashMap<>();
        HashMap<String, Object> foreignCoin = new HashMap<>();
        foreignCoin.put("id", "fake");
        foreignCoin.put("currency", "PENY");
        foreignCoin.put("value", 8.0);
        foreignCoin.put("from", "No BODY");
        coinMap2.put("0", foreignCoin);
        firestoreTest.collection("foreignCoins").document("espresso2@test.com").set(coinMap2);

    }

    @Test
    public void expiredCoinsTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        sleep(2000);

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
        appCompatEditText.perform(replaceText("espresso2@test.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(R.id.AutPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.cardView2),
                                        0),
                                0),
                        isDisplayed()));
        appCompatEditText6.perform(replaceText("123456"), closeSoftKeyboard());

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

        // Use Barista library's method to grant location permission.
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

        sleep(500);

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

        sleep(3000);

        // The local coin of currency SHIL should be expired
        assertDisplayed(R.id.localSHIL, "0");
        assertDisplayed(R.id.valSHIL, "0.000");
        // The local coin of currency DOLR should be expired
        assertDisplayed(R.id.localDOLR, "0");
        assertDisplayed(R.id.valDOLR, "0.000");
        // The foreign coin of currency PENY should be exipred
        assertDisplayed(R.id.foreignPENY, "0");
        assertDisplayed(R.id.valPENY, "0.000");
        // The local coin list should be empty
        assertNotDisplayed(R.id.coinsList, "SHIL");
        assertNotDisplayed(R.id.coinsList, "DOLR");
        // The foreign coin list should be empty
        assertNotDisplayed(R.id.foreignCoinsList, "PENY");

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
