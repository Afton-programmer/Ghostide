package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Lets a plugin take over a single tap on a file (or folder) in the file manager, instead of the
 * IDE opening it with one of its built-in viewers.
 *
 * <p>Register an implementation at {@link PluginUiExtensionPoints#FILE_CLICK_HANDLER}. On every tap
 * the host asks each handler, in descending priority order, whether it {@link #supports claims} the
 * path; the first one that says {@code true} wins and receives {@link #onFileClick
 * FileClickContext}, and the built-in flow (open in editor, image/audio viewer, apk install, ...)
 * is skipped for that tap. Returning {@code false} from {@code supports}, or not being registered
 * at all, leaves the IDE's own behaviour untouched.
 *
 * <pre>{@code
 * public final class SvgToPngHandler implements FileClickHandler {
 *   @Override public boolean supports(String path, String name, boolean isDirectory) {
 *     return !isDirectory && name.toLowerCase().endsWith(".svg");
 *   }
 *
 *   @Override public void onFileClick(FileClickContext context) {
 *     ui.confirm(
 *         "Convert " + context.name() + "?", context.path(), ok -> {
 *           if (ok) convert(context.file());
 *         });
 *   }
 * }
 *
 * // in activate():
 * context.registerDisposable(context.getExtensions().register(
 *     PluginUiExtensionPoints.FILE_CLICK_HANDLER, new SvgToPngHandler()));
 * }</pre>
 */
public interface FileClickHandler {

  /**
   * Whether this handler owns a tap on the given entry. Keep it fast and free of I/O on the UI
   * thread — it runs for every tap on every row. Return {@code false} for paths you don't handle so
   * the next handler (or the built-in flow) takes over.
   *
   * @param path absolute path of the tapped file or directory
   * @param name display name of the tapped entry
   * @param isDirectory {@code true} when the entry is a directory
   */
  boolean supports(String path, String name, boolean isDirectory);

  /**
   * Handles a tap this handler claimed. Called on the UI thread; show your own dialog or start your
   * own screen here. Exceptions thrown here are swallowed and the tap falls through to the built-in
   * flow, so a broken handler can never block the file manager.
   */
  void onFileClick(FileClickContext context);
}
