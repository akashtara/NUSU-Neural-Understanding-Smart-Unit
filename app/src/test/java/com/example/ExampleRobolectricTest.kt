package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.api.PlatformManager
import com.example.api.PlatformType
import com.example.services.NusuSoundManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NUSU", appName)
  }

  @Test
  fun `verify platform manager initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    PlatformManager.initialize(context)
    val current = PlatformManager.currentPlatform.value
    assertNotNull(current)
    assertEquals(PlatformType.ANDROID_MOBILE, current?.type)
    assertFalse(current?.isExternalDisplayConnected ?: true)
  }
}
