package ir.hanzodev1375.ghostide.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

import ir.hanzodev1375.ghostide.glide.apkicon.ApkIconModelLoader;
import ir.hanzodev1375.ghostide.glide.gpl.GplIcon;
import ir.hanzodev1375.ghostide.glide.gpl.GplIconLoader;
import ir.hanzodev1375.ghostide.glide.music.Mp3CoverLoaderFactory;
import ir.hanzodev1375.ghostide.glide.svg.SvgDecoder;
import ir.hanzodev1375.ghostide.glide.svg.SvgDrawableTranscoder;
import ir.hanzodev1375.ghostide.glide.xml.VectorModel;
import ir.hanzodev1375.ghostide.glide.xml.VectorModelLoaderFactory;

@GlideModule
public class AppGlide extends AppGlideModule {

  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    registry
        .register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
        .append(InputStream.class, SVG.class, new SvgDecoder())
        .append(GplIcon.class, InputStream.class, new GplIconLoader.Factory())
        .append(String.class, Bitmap.class, new Mp3CoverLoaderFactory())
        .append(String.class, Drawable.class, new ApkIconModelLoader.Factory(context))
        .append(VectorModel.class, Drawable.class, new VectorModelLoaderFactory(context));
  }
}
