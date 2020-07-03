
package de.tudarmstadt.informatik.hostage.profile;
import android.graphics.Bitmap;
import android.os.Parcel;


import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;


import de.tudarmstadt.informatik.hostage.model.Profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class ProfileTest {
    private String mText;
    private String mLabel;
    private int mId;
    private boolean mActivated;
    transient private Bitmap mIcon;
    transient private int mIconId;
    private String mIconName;
    private String mIconPath;

    private boolean mEditable = false;
    private boolean mIsRandom = false;

    private HashMap<String, Boolean> mActiveProtocols = new HashMap<String, Boolean>();
    private String mGhostPorts = "";
    private boolean mGhostActive = false;
    private boolean mShowTooltip = false;

    Profile profileBitmap;
    Profile profilePath;

    @Before
    public void setUp() {
          mText = "androidTest";
          mLabel = "testLabel";
          mId = 2;
          mActivated = true;
          mIcon = null;
          mIconId = 3;
          mIconName = "testIcon";
          mIconPath = "testPath";
          mEditable = false;
          mIsRandom = false;
          mActiveProtocols = new HashMap<String, Boolean>();
          mActiveProtocols.put("androidTest",true);
          mGhostPorts = "";
          mGhostActive = false;
          mShowTooltip = false;

         profileBitmap = new Profile(mId, mLabel,mText, mIcon, mEditable);
         profilePath = new Profile(mId, mLabel,mText, mIconPath, mEditable);
    }

    @Test
    public void emptyConstructorTest() {
        Profile profile = new Profile();

        assertTrue(profile.mEditable);
        assertFalse(profile.mActivated);
        assertTrue(profile.mId == -1);

    }

    @Test
    public void bitMapConstructorTest(){

        assertTrue(profileBitmap.mId == 2) ;
        assertEquals("testLabel",profileBitmap.mLabel);
        assertTrue(profileBitmap.mText.equals("androidTest"));
        assertNull(profileBitmap.mIcon);
        assertFalse(profileBitmap.mEditable);

    }

    @Test
    public void pathConstructorTest(){

        assertTrue(profilePath.mId == 2) ;
        assertEquals("testLabel",profilePath.mLabel);
        assertTrue(profilePath.mText.equals("androidTest"));
        assertTrue(profilePath.mIconPath.equals("testPath"));
        assertFalse(profilePath.mEditable);

    }

    @Test
    public void checkProfileIsParcableTest(){

        Parcel parcel = Parcel.obtain();
        parcel.writeString(mText);
        parcel.writeString(mLabel);
        parcel.writeInt(mId);
        parcel.writeByte((byte) (mActivated ? 1 : 0));
        parcel.writeInt(mIconId);
        parcel.writeString(mIconName);
        parcel.writeString(mIconPath);
        parcel.writeByte((byte) (mEditable ? 1 : 0));
        parcel.writeSerializable(mActiveProtocols);
        parcel.setDataPosition(0);


        Profile parceled = new Profile(parcel);

        assertTrue(parceled.mId == 2) ;
        assertTrue(parceled.mIconId == 3) ;
        assertTrue(parceled.mLabel.equals("testLabel"));
        assertTrue(parceled.mText.equals("androidTest"));
        assertTrue(parceled.mIconPath.equals("testPath"));
        assertTrue(mActivated);
        assertFalse(mEditable);
        assertFalse(parceled.mEditable);

    }
}
