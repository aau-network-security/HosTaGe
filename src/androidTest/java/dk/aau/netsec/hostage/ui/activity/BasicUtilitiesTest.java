package dk.aau.netsec.hostage.ui.activity;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dk.aau.netsec.hostage.R;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicUtilitiesTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void basicUtilitiesTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction button = onView(
                allOf(withId(android.R.id.button1), withText("Agree"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.home_switch_connection),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        switch_.perform(click());

        ViewInteraction imageView = onView(
                allOf(withId(R.id.home_button_connection_info),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        imageView.perform(click());

        ViewInteraction button2 = onView(
                allOf(withId(android.R.id.button1), withText("Show records"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        button2.perform(click());

        ViewInteraction linearLayout = onView(
                allOf(withContentDescription("Records, Open navigation drawer"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.widget.ActionBarView")),
                                        childAtPosition(
                                                withClassName(is("com.android.internal.widget.ActionBarContainer")),
                                                0)),
                                0),
                        isDisplayed()));
        linearLayout.perform(click());

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.left_drawer),
                        childAtPosition(
                                withId(R.id.drawer_layout),
                                1)))
                .atPosition(4);
        linearLayout2.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.services_button_connection_info),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        imageView2.perform(click());

        ViewInteraction button3 = onView(
                allOf(withId(android.R.id.button1), withText("Show records"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        button3.perform(click());

        ViewInteraction linearLayout3 = onView(
                allOf(withContentDescription("Records, Open navigation drawer"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.widget.ActionBarView")),
                                        childAtPosition(
                                                withClassName(is("com.android.internal.widget.ActionBarContainer")),
                                                0)),
                                0),
                        isDisplayed()));
        linearLayout3.perform(click());

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.left_drawer),
                        childAtPosition(
                                withId(R.id.drawer_layout),
                                1)))
                .atPosition(6);
        linearLayout4.perform(click());

        DataInteraction linearLayout5 = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
                .atPosition(1);
        linearLayout5.perform(click());

        DataInteraction linearLayout6 = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
                .atPosition(1);
        linearLayout6.perform(click());

        ViewInteraction relativeLayout = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(android.R.id.list),
                                1),
                        1),
                        isDisplayed()));
        relativeLayout.check(matches(isDisplayed()));

        ViewInteraction linearLayout7 = onView(
                allOf(withContentDescription("Settings, Open navigation drawer"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.widget.ActionBarView")),
                                        childAtPosition(
                                                withClassName(is("com.android.internal.widget.ActionBarContainer")),
                                                0)),
                                0),
                        isDisplayed()));
        linearLayout7.perform(click());

        DataInteraction linearLayout8 = onData(anything())
                .inAdapterView(allOf(withId(R.id.left_drawer),
                        childAtPosition(
                                withId(R.id.drawer_layout),
                                1)))
                .atPosition(6);
        linearLayout8.perform(click());

        ViewInteraction linearLayout9 = onView(
                allOf(withContentDescription("Settings, Open navigation drawer"),
                        childAtPosition(
                                allOf(withClassName(is("com.android.internal.widget.ActionBarView")),
                                        childAtPosition(
                                                withClassName(is("com.android.internal.widget.ActionBarContainer")),
                                                0)),
                                0),
                        isDisplayed()));
        linearLayout9.perform(click());

        DataInteraction linearLayout10 = onData(anything())
                .inAdapterView(allOf(withId(R.id.left_drawer),
                        childAtPosition(
                                withId(R.id.drawer_layout),
                                1)))
                .atPosition(1);
        linearLayout10.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction relativeLayout2 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.threatmapfragment),
                                0),
                        0),
                        isDisplayed()));
        relativeLayout2.check(matches(isDisplayed()));
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
