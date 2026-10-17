package ir.hanzodev1375.components.glsl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Ready-made GLSL ES 2.0 fragment shaders shared by every {@link GLSLShaderView} user.
 *
 * <p>All shaders follow the same contract so they can be swapped without touching the renderer:
 * they receive {@code uResolution} (physical pixels), {@code uTime} (seconds since start) and the
 * three theme colors {@code uColorA}/{@code uColorB}/{@code uColorC} (linear RGB in 0..1). The
 * colors are always supplied by the caller from {@code M3Theme}, never hard-coded here.
 */
public final class GlslEffects {

  /** Living nebula built from layered value noise. Suitable for full-screen backgrounds. */
  public static final String NEBULA = "nebula";

  /** Soft color bands drifting horizontally, like northern lights. */
  public static final String AURORA = "aurora";

  /** Classic plasma field from interfering sine waves. */
  public static final String PLASMA = "plasma";

  /** Slowly scrolling technical grid, readable behind dark UI. */
  public static final String GRID = "grid";

  /** Radial breathing aura, used behind logos and loading overlays. */
  public static final String AURA = "aura";

  private static final String VERTEX_SHADER =
      "attribute vec2 aPosition;\n"
          + "varying vec2 vUv;\n"
          + "void main() {\n"
          + "  vUv = aPosition * 0.5 + 0.5;\n"
          + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
          + "}\n";

  private static final String COMMON_HEADER =
      "precision mediump float;\n"
          + "uniform vec2 uResolution;\n"
          + "uniform float uTime;\n"
          + "uniform vec3 uColorA;\n"
          + "uniform vec3 uColorB;\n"
          + "uniform vec3 uColorC;\n"
          + "varying vec2 vUv;\n";

  private static final String HASH_NOISE =
      "float hash(vec2 p) {\n"
          + "  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);\n"
          + "}\n"
          + "float noise(vec2 p) {\n"
          + "  vec2 i = floor(p);\n"
          + "  vec2 f = fract(p);\n"
          + "  vec2 u = f * f * (3.0 - 2.0 * f);\n"
          + "  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),\n"
          + "             mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);\n"
          + "}\n"
          + "float fbm(vec2 p) {\n"
          + "  float v = 0.0;\n"
          + "  float a = 0.5;\n"
          + "  for (int i = 0; i < 5; i++) {\n"
          + "    v += a * noise(p);\n"
          + "    p *= 2.0;\n"
          + "    a *= 0.5;\n"
          + "  }\n"
          + "  return v;\n"
          + "}\n";

  private static final String ASPECT =
      "float aspect() {\n" + "  return uResolution.x / max(uResolution.y, 1.0);\n" + "}\n";

  private static final String FRAG_NEBULA =
      COMMON_HEADER
          + HASH_NOISE
          + ASPECT
          + "void main() {\n"
          + "  vec2 uv = vUv;\n"
          + "  uv.x *= aspect();\n"
          + "  float t = uTime * 0.04;\n"
          + "  vec2 q = uv * 2.6;\n"
          + "  float n1 = fbm(q + vec2(t, -t));\n"
          + "  float n2 = fbm(q * 1.7 + vec2(-t * 0.7, t * 0.9) + n1);\n"
          + "  vec3 col = mix(uColorA, uColorB, clamp(n1, 0.0, 1.0));\n"
          + "  col = mix(col, uColorC, clamp(n2 * n2 * 1.4, 0.0, 1.0) * 0.6);\n"
          + "  float d = distance(uv, vec2(aspect() * 0.5, 0.5));\n"
          + "  col *= smoothstep(1.35, 0.15, d);\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private static final String FRAG_AURORA =
      COMMON_HEADER
          + ASPECT
          + "void main() {\n"
          + "  vec2 uv = vUv;\n"
          + "  uv.x *= aspect();\n"
          + "  float t = uTime * 0.12;\n"
          + "  float band1 = sin(uv.x * 2.0 + t) * 0.5 + 0.5;\n"
          + "  float band2 = sin(uv.x * 3.1 - uv.y * 2.0 - t * 1.3) * 0.5 + 0.5;\n"
          + "  float band3 = sin(uv.x * 1.3 + uv.y * 3.0 + t * 0.7) * 0.5 + 0.5;\n"
          + "  vec3 col = uColorA;\n"
          + "  col += uColorB * band1 * 0.45;\n"
          + "  col += uColorC * band2 * 0.30;\n"
          + "  col *= 0.55 + 0.45 * band3;\n"
          + "  float edge = smoothstep(1.15, 0.15, abs(uv.y - 0.5) * 1.5);\n"
          + "  gl_FragColor = vec4(col * edge, 1.0);\n"
          + "}\n";

  private static final String FRAG_PLASMA =
      COMMON_HEADER
          + ASPECT
          + "void main() {\n"
          + "  vec2 uv = vUv;\n"
          + "  uv.x *= aspect();\n"
          + "  float t = uTime * 0.18;\n"
          + "  float v = sin(uv.x * 4.0 + t)\n"
          + "          + sin(uv.y * 5.0 - t * 0.8)\n"
          + "          + sin((uv.x + uv.y) * 4.0 + t * 1.2);\n"
          + "  v = v / 6.0 + 0.5;\n"
          + "  vec3 col = mix(uColorA, uColorB, clamp(v, 0.0, 1.0));\n"
          + "  col = mix(col, uColorC, pow(clamp(v, 0.0, 1.0), 3.0) * 0.7);\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private static final String FRAG_GRID =
      COMMON_HEADER
          + ASPECT
          + "void main() {\n"
          + "  vec2 uv = vUv;\n"
          + "  uv.x *= aspect();\n"
          + "  float t = uTime * 0.06;\n"
          + "  vec2 g = abs(fract(uv * 14.0 + vec2(t, -t * 0.6)) - 0.5);\n"
          + "  float line = smoothstep(0.46, 0.5, max(g.x, g.y));\n"
          + "  vec2 g2 = abs(fract(uv * 3.0 + vec2(t * 0.4, -t * 0.25)) - 0.5);\n"
          + "  float line2 = smoothstep(0.47, 0.5, max(g2.x, g2.y));\n"
          + "  vec3 col = uColorA;\n"
          + "  col = mix(col, uColorB, line * 0.35 + line2 * 0.5);\n"
          + "  float glow = 0.5 + 0.5 * sin(uTime * 0.5);\n"
          + "  col += uColorC * line2 * 0.25 * glow;\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private static final String FRAG_AURA =
      COMMON_HEADER
          + HASH_NOISE
          + ASPECT
          + "void main() {\n"
          + "  vec2 uv = vUv;\n"
          + "  vec2 p = uv - 0.5;\n"
          + "  p.x *= aspect();\n"
          + "  float r = length(p) * 2.0;\n"
          + "  float breath = 0.5 + 0.5 * sin(uTime * 0.9);\n"
          + "  float halo = smoothstep(1.0, 0.0, r);\n"
          + "  float ring = smoothstep(0.32, 0.0, abs(r - (0.55 + 0.08 * breath)));\n"
          + "  vec3 col = mix(uColorA, uColorB, halo * 0.6);\n"
          + "  col += uColorC * ring * 0.8;\n"
          + "  col *= 0.35 + 0.65 * halo;\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private GlslEffects() {}

  /** Returns the vertex shader shared by every effect. */
  @NonNull
  public static String vertexShader() {
    return VERTEX_SHADER;
  }

  /**
   * Returns the fragment shader for a named effect. Unknown names fall back to {@link #AURORA} so
   * callers never have to guard against a null shader.
   */
  @NonNull
  public static String fragmentShader(@Nullable String name) {
    String key = name == null ? "" : name.toLowerCase(Locale.US);
    switch (key) {
      case NEBULA:
        return FRAG_NEBULA;
      case PLASMA:
        return FRAG_PLASMA;
      case GRID:
        return FRAG_GRID;
      case AURA:
        return FRAG_AURA;
      case AURORA:
      default:
        return FRAG_AURORA;
    }
  }
}
