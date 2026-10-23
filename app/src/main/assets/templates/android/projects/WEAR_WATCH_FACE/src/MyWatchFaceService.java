package ${PACKAGE};

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class MyWatchFaceService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new MyEngine();
    }

    private class MyEngine extends Engine {
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }
    }
}
