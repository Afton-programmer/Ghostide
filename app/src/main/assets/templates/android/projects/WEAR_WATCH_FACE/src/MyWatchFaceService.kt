package ${PACKAGE}

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class MyWatchFaceService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return MyEngine()
    }

    inner class MyEngine : Engine() {
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }
    }
}
