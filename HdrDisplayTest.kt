package com.bistarma.hdrvideo

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.view.Surface

/** All error checks are removed to make it readable.
 * EGL_EXT_gl_colorspace_bt2020_linear doesn't trigger hdr output on my test devices
 * EGL_EXT_gl_colorspace_bt2020_hlg doesn't seem to have any support
 *
 * Surface comes from a SurfaceView. I couldn't trigger hdr with anything else
 * Looks like peak brightness depends on physical size of SurfaceView
 */
class HdrDisplayTest(surface: Surface) {


    var eglDisplay: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    var eglConfig: EGLConfig? = null


    // https://registry.khronos.org/EGL/extensions/KHR/EGL_KHR_gl_colorspace.txt
    private val EGL_GL_COLORSPACE_KHR: Int = 0x309D
    // https://registry.khronos.org/EGL/extensions/EXT/EGL_EXT_gl_colorspace_bt2020_linear.txt
    private val EGL_GL_COLORSPACE_BT2020_PQ_EXT: Int = 0x3340


    init {
        //get display
        val version = IntArray(2)
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)


        //create context
        eglConfig = getConfig()     // RGBA_1010102
        val attrib3_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, attrib3_list, 0)


        // create window
        val windowAttribs = intArrayOf(EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT, EGL14.EGL_NONE, EGL14.EGL_NONE)
        val window = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, windowAttribs, 0)


        // pass hdr metadata
        // https://registry.khronos.org/EGL/extensions/EXT/EGL_EXT_surface_SMPTE2086_metadata.txt
        val windowAttribsSMPTE = intArrayOf(0x3341, 0x3342, 0x3343, 0x3344, 0x3345, 0x3346, 0x3347, 0x3348, 0x3349, 0x334A)
        val chromacities = intArrayOf(35400, 14600, 8500, 39850, 6550, 2300, 15635, 16450)
        for (i in 0..7) EGL14.eglSurfaceAttrib(eglDisplay, window, windowAttribsSMPTE[i], chromacities[i])
        EGL14.eglSurfaceAttrib(eglDisplay, window, windowAttribsSMPTE[8], 1000*50000) // nits * 50000
        EGL14.eglSurfaceAttrib(eglDisplay, window, windowAttribsSMPTE[9], 0)


        // swap buffers
        // this step should trigger hdr
        EGL14.eglMakeCurrent(eglDisplay, window, window, eglContext)
        EGL14.eglSwapBuffers(eglDisplay, window)
    }

    private fun getConfig(): EGLConfig? {
        val EGL_CONFIG_ATTRIBUTES_RGBA_1010102 = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE,  /* redSize= */10,
            EGL14.EGL_GREEN_SIZE,  /* greenSize= */10,
            EGL14.EGL_BLUE_SIZE,  /* blueSize= */10,
            EGL14.EGL_ALPHA_SIZE,  /* alphaSize= */2,
            EGL14.EGL_DEPTH_SIZE,  /* depthSize= */0,
            EGL14.EGL_STENCIL_SIZE,  /* stencilSize= */0,
            EGL14.EGL_NONE
        )

        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        EGL14.eglChooseConfig(
                eglDisplay,
                EGL_CONFIG_ATTRIBUTES_RGBA_1010102,
                0,
                eglConfigs,
                0,
                1, IntArray(1),
                0
        )
        return eglConfigs[0]
    }
}