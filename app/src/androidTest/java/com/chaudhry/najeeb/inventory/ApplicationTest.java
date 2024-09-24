package com.chaudhry.najeeb.inventory;

import android.app.Application;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Application app = ApplicationProvider.getApplicationContext();
        assertEquals("com.chaudhry.najeeb.inventory", app.getPackageName());
    }
}