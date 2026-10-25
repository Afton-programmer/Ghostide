package ir.hanzodev1375.ghostide.project.android;

import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;

public enum AndroidTemplate {
  // Phone & Tablet
  PHONE_EMPTY_VIEWS(
      AndroidCategory.PHONE_TABLET,
      "Empty Views Activity",
      "Creates an empty activity configured for XML layouts, Material 3, and ViewBinding. Supports both Kotlin and Java.",
      21,
      true,
      false),
  PHONE_EMPTY_COMPOSE(
      AndroidCategory.PHONE_TABLET,
      "Empty Activity (Jetpack Compose)",
      "Creates an activity with a single Composable hierarchy using Jetpack Compose and Material 3 design tokens. (Kotlin only)",
      24,
      false,
      true),
  PHONE_BASIC_VIEWS(
      AndroidCategory.PHONE_TABLET,
      "Basic Views Activity",
      "Creates an activity with a Material 3 App Bar, Floating Action Button, and Jetpack Navigation between fragments.",
      21,
      true,
      false),
  PHONE_BOTTOM_NAV(
      AndroidCategory.PHONE_TABLET,
      "Bottom Navigation Views Activity",
      "Creates an activity with a standard BottomNavigationView and 3 Jetpack Navigation destination fragments.",
      21,
      true,
      false),
  PHONE_NAVIGATION_DRAWER(
      AndroidCategory.PHONE_TABLET,
      "Navigation Drawer Views Activity",
      "Creates an activity with a collapsible Navigation Drawer (DrawerLayout + NavigationView) and header.",
      21,
      true,
      false),
  PHONE_TABBED(
      AndroidCategory.PHONE_TABLET,
      "Tabbed Views Activity",
      "Creates an activity with TabLayout and ViewPager2 for horizontal page swiping.",
      21,
      true,
      false),
  PHONE_FULLSCREEN(
      AndroidCategory.PHONE_TABLET,
      "Fullscreen Activity",
      "Creates an activity that hides system bars with WindowInsetsControllerCompat for an immersive full-screen experience.",
      21,
      true,
      false),
  PHONE_NO_ACTIVITY(
      AndroidCategory.PHONE_TABLET,
      "No Activity",
      "Creates a minimal Android application structure without any default Activity.",
      21,
      true,
      false),

  // Wear OS
  WEAR_BLANK_VIEWS(
      AndroidCategory.WEAR_OS,
      "Blank Wear OS Activity",
      "Creates a standalone Wear OS activity with wearable layout and ambient display support.",
      30,
      true,
      false),
  WEAR_COMPOSE(
      AndroidCategory.WEAR_OS,
      "Wear OS Compose App",
      "Creates a Wear OS app using Wear Compose Material, TimeText, and ScalingLazyColumn. (Kotlin only)",
      30,
      false,
      true),
  WEAR_WATCH_FACE(
      AndroidCategory.WEAR_OS,
      "Watch Face Service",
      "Creates a modern Wear OS WatchFaceService wallpaper provider with CanvasRenderer support.",
      30,
      true,
      false),
  WEAR_TILE_SERVICE(
      AndroidCategory.WEAR_OS,
      "Wear OS Tile Service",
      "Creates a Wear OS Tile service using ProtoLayout and Wear Tiles for glanceable quick info.",
      30,
      true,
      false),

  // Android TV
  TV_LEANBACK(
      AndroidCategory.ANDROID_TV,
      "Android TV Leanback Activity",
      "Creates an Android TV application using the Leanback library with BrowseSupportFragment and CardPresenter.",
      21,
      true,
      false),
  TV_COMPOSE(
      AndroidCategory.ANDROID_TV,
      "Android TV Compose Activity",
      "Creates a modern Android TV app with Jetpack Compose for TV (androidx.tv:tv-material). (Kotlin only)",
      24,
      false,
      true),

  // Automotive
  AUTO_CAR_APP(
      AndroidCategory.AUTOMOTIVE,
      "Car App Service",
      "Creates an Android Auto & AAOS CarAppService with Session, Screen, and PaneTemplate navigation.",
      29,
      true,
      false),
  AUTO_MEDIA_APP(
      AndroidCategory.AUTOMOTIVE,
      "Automotive Media Service",
      "Creates a MediaLibraryService compliant with Android Auto and Automotive media browser architecture.",
      29,
      true,
      false);

  private final AndroidCategory category;
  private final String title;
  private final String description;
  private final int defaultMinSdk;
  private final boolean supportsJava;
  private final boolean supportsCompose;

  AndroidTemplate(
      AndroidCategory category,
      String title,
      String description,
      int defaultMinSdk,
      boolean supportsJava,
      boolean supportsCompose) {
    this.category = category;
    this.title = title;
    this.description = description;
    this.defaultMinSdk = defaultMinSdk;
    this.supportsJava = supportsJava;
    this.supportsCompose = supportsCompose;
  }

  public AndroidCategory getCategory() {
    return category;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getDefaultMinSdk() {
    return defaultMinSdk;
  }

  public boolean isSupportsJava() {
    return supportsJava;
  }

  public boolean isSupportsCompose() {
    return supportsCompose;
  }

  public int getMockupRes() {
    return switch (this) {
      case PHONE_EMPTY_VIEWS -> R.drawable.ic_mockup_empty_views;
      case PHONE_EMPTY_COMPOSE -> R.drawable.ic_mockup_compose;
      case PHONE_BASIC_VIEWS -> R.drawable.ic_mockup_basic_views;
      case PHONE_BOTTOM_NAV -> R.drawable.ic_mockup_bottom_nav;
      case PHONE_NAVIGATION_DRAWER -> R.drawable.ic_mockup_nav_drawer;
      case PHONE_TABBED -> R.drawable.ic_mockup_tabbed;
      case PHONE_FULLSCREEN -> R.drawable.ic_mockup_fullscreen;
      case PHONE_NO_ACTIVITY -> R.drawable.ic_mockup_no_activity;
      case WEAR_BLANK_VIEWS, WEAR_COMPOSE, WEAR_WATCH_FACE, WEAR_TILE_SERVICE -> R.drawable.ic_mockup_wear;
      case TV_LEANBACK, TV_COMPOSE -> R.drawable.ic_mockup_tv;
      case AUTO_CAR_APP, AUTO_MEDIA_APP -> R.drawable.ic_mockup_auto;
    };
  }

  public String getUiEngine() {
    if (supportsCompose) return "Jetpack Compose (Material 3)";
    return switch (category) {
      case PHONE_TABLET -> "Material 3 XML Views (ViewBinding)";
      case WEAR_OS -> "Wear OS Views + Ambient Display";
      case ANDROID_TV -> "Android TV (Leanback SDK)";
      case AUTOMOTIVE -> "Android for Cars (CarAppService)";
    };
  }

  public String getArchitecture() {
    return switch (this) {
      case PHONE_EMPTY_VIEWS -> "Single Activity · ViewBinding · M3 Tokens";
      case PHONE_EMPTY_COMPOSE -> "Single Activity · Compose Scaffolding · Material 3";
      case PHONE_BASIC_VIEWS -> "Single Activity + Fragment Navigation + FAB";
      case PHONE_BOTTOM_NAV -> "Single Activity + BottomNavigationView + 3 Fragments";
      case PHONE_NAVIGATION_DRAWER -> "Single Activity + DrawerLayout + NavigationView";
      case PHONE_TABBED -> "Single Activity + TabLayout + ViewPager2";
      case PHONE_FULLSCREEN -> "Single Activity · WindowInsetsControllerCompat";
      case PHONE_NO_ACTIVITY -> "Minimal Application Manifest Structure";
      case WEAR_BLANK_VIEWS -> "WearableActivity + AmbientMode Callback";
      case WEAR_COMPOSE -> "Wear Compose Material + ScalingLazyColumn";
      case WEAR_WATCH_FACE -> "WatchFaceService + CanvasRenderer + Complications";
      case WEAR_TILE_SERVICE -> "ProtoLayout TileService + Glanceable Timeline";
      case TV_LEANBACK -> "BrowseSupportFragment + ArrayObjectAdapter";
      case TV_COMPOSE -> "androidx.tv:tv-material + Focusable LazyRows";
      case AUTO_CAR_APP -> "CarAppService + Session + PaneTemplate";
      case AUTO_MEDIA_APP -> "MediaLibraryService + MediaSessionCompat";
    };
  }

  public String getGeneratedFilesSummary() {
    return switch (this) {
      case PHONE_EMPTY_VIEWS -> "MainActivity, activity_main.xml, build.gradle.kts";
      case PHONE_EMPTY_COMPOSE -> "MainActivity.kt, ui/theme/Theme.kt, build.gradle.kts";
      case PHONE_BASIC_VIEWS -> "MainActivity, FirstFragment, SecondFragment, nav_graph.xml";
      case PHONE_BOTTOM_NAV -> "MainActivity, ui/home, ui/dashboard, ui/notifications";
      case PHONE_NAVIGATION_DRAWER -> "MainActivity, nav_header_main.xml, activity_main.xml";
      case PHONE_TABBED -> "MainActivity, SectionsPagerAdapter, PlaceholderFragment";
      case PHONE_FULLSCREEN -> "MainActivity, activity_fullscreen.xml, colors.xml";
      case PHONE_NO_ACTIVITY -> "AndroidManifest.xml, build.gradle.kts, libs.versions.toml";
      case WEAR_BLANK_VIEWS -> "MainActivity, activity_main.xml, AmbientModeSupport";
      case WEAR_COMPOSE -> "MainActivity.kt, theme/Theme.kt, TimeText.kt";
      case WEAR_WATCH_FACE -> "MyWatchFaceService, WatchFaceRenderer, complications.xml";
      case WEAR_TILE_SERVICE -> "MainTileService, tile_layout.xml, ProtoLayout";
      case TV_LEANBACK -> "MainActivity, MainFragment, CardPresenter, Movie.java";
      case TV_COMPOSE -> "MainActivity.kt, TvTheme.kt, FeaturedCarousel.kt";
      case AUTO_CAR_APP -> "MyCarAppService, MainScreen, session.xml";
      case AUTO_MEDIA_APP -> "MyMediaLibraryService, media_items.xml";
    };
  }

  public static List<AndroidTemplate> getByCategory(AndroidCategory category) {
    List<AndroidTemplate> list = new ArrayList<>();
    for (AndroidTemplate template : values()) {
      if (template.category == category) {
        list.add(template);
      }
    }
    return list;
  }
}
