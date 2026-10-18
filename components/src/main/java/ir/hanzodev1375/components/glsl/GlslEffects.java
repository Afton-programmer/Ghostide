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
 *
 * <p>Precision is {@code highp} where the device guarantees it (falling back to {@code mediump}
 * otherwise) so the layered noise and star fields stay free of banding.
 */
public final class GlslEffects {

  /** Flowing two-layer noise field with a soft specular highlight. */
  public static final String LIQUID = "liquid";

  /** Stacked luminous color ribbons drifting sideways, like silk in the wind. */
  public static final String SILK = "silk";

  /** Slowly rotating nebula with a twinkling star field. */
  public static final String GALAXY = "galaxy";

  private static final String VERTEX_SHADER =
      "attribute vec2 aPosition;\n"
          + "varying vec2 vUv;\n"
          + "void main() {\n"
          + "  vUv = aPosition * 0.5 + 0.5;\n"
          + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
          + "}\n";

  private static final String COMMON_HEADER =
      "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
          + "precision highp float;\n"
          + "#else\n"
          + "precision mediump float;\n"
          + "#endif\n"
          + "uniform vec2 uResolution;\n"
          + "uniform float uTime;\n"
          + "uniform vec3 uColorA;\n"
          + "uniform vec3 uColorB;\n"
          + "uniform vec3 uColorC;\n"
          + "varying vec2 vUv;\n";

  private static final String HASH21 =
      "float hash21(vec2 p) {\n"
          + "  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);\n"
          + "}\n";

  /** Quintic value noise + rotated fBm, used by {@link #LIQUID}. */
  private static final String LIQUID_NOISE =
      HASH21
          + "float vnoise(vec2 p) {\n"
          + "  vec2 i = floor(p);\n"
          + "  vec2 f = fract(p);\n"
          + "  vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);\n"
          + "  return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), u.x),\n"
          + "             mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x), u.y);\n"
          + "}\n"
          + "float fbm(vec2 p) {\n"
          + "  float v = 0.0;\n"
          + "  float a = 0.5;\n"
          + "  mat2 m = mat2(0.8, -0.6, 0.6, 0.8);\n"
          + "  for (int i = 0; i < 4; i++) {\n"
          + "    v += a * vnoise(p);\n"
          + "    p = m * p * 2.03 + 11.7;\n"
          + "    a *= 0.5;\n"
          + "  }\n"
          + "  return v / 0.9375;\n"
          + "}\n";

  /** Smooth value noise + simple fBm, used by {@link #GALAXY}. */
  private static final String GALAXY_NOISE =
      HASH21
          + "float vnoise(vec2 p) {\n"
          + "  vec2 i = floor(p);\n"
          + "  vec2 f = fract(p);\n"
          + "  vec2 u = f * f * (3.0 - 2.0 * f);\n"
          + "  return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), u.x),\n"
          + "             mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x), u.y);\n"
          + "}\n"
          + "float fbm(vec2 p) {\n"
          + "  float v = 0.0;\n"
          + "  float a = 0.5;\n"
          + "  for (int i = 0; i < 5; i++) {\n"
          + "    v += a * vnoise(p);\n"
          + "    p = p * 2.02 + 7.3;\n"
          + "    a *= 0.5;\n"
          + "  }\n"
          + "  return v;\n"
          + "}\n";

  private static final String FRAG_LIQUID =
      COMMON_HEADER
          + LIQUID_NOISE
          + "void main() {\n"
          + "  vec2 fragCoord = vUv * uResolution;\n"
          + "  vec2 uv = (fragCoord - 0.5 * uResolution.xy) / uResolution.y;\n"
          + "  float t = uTime * 0.06;\n"
          + "  vec2 p = uv * 1.8;\n"
          + "  vec2 q = vec2(fbm(p + vec2(0.0, t)), fbm(p + vec2(5.2, 1.3) - t));\n"
          + "  float f = fbm(p + 3.2 * q + t * 0.5);\n"
          + "  vec3 col = mix(uColorA, uColorB, smoothstep(0.25, 0.85, f));\n"
          + "  col = mix(col, uColorC, smoothstep(0.55, 1.05, length(q) * 1.1 + f * 0.25) * 0.9);\n"
          + "  float e = 0.02;\n"
          + "  float fx = fbm(p + 3.2 * q + t * 0.5 + vec2(e, 0.0)) - f;\n"
          + "  float fy = fbm(p + 3.2 * q + t * 0.5 + vec2(0.0, e)) - f;\n"
          + "  float lit = clamp(0.5 + (fx - fy) * 9.0, 0.0, 1.0);\n"
          + "  col += uColorB * 0.10 * lit * lit;\n"
          + "  col *= 0.85 + 0.45 * f;\n"
          + "  col *= 1.0 - 0.45 * dot(uv, uv);\n"
          + "  float g = fract(sin(dot(fragCoord, vec2(12.9898, 78.233))) * 43758.5453);\n"
          + "  col += (g - 0.5) / 128.0;\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private static final String FRAG_SILK =
      COMMON_HEADER
          + "void main() {\n"
          + "  vec2 fragCoord = vUv * uResolution;\n"
          + "  vec2 uv = fragCoord / uResolution.xy;\n"
          + "  float asp = uResolution.x / uResolution.y;\n"
          + "  float t = uTime * 0.22;\n"
          + "  vec3 col = mix(uColorA * 1.6, uColorA * 0.5, uv.y);\n"
          + "  for (int i = 0; i < 7; i++) {\n"
          + "    float fi = float(i);\n"
          + "    float k = fi / 6.0;\n"
          + "    float x = uv.x * asp * 1.6;\n"
          + "    float y = 0.50 + (k - 0.5) * 0.30\n"
          + "            + 0.17 * sin(x * (1.3 + k * 0.5) + t * (0.7 + k * 0.25) + fi * 1.9)\n"
          + "            + 0.07 * sin(x * (3.1 + k) - t * 1.1 + fi * 2.7);\n"
          + "    float d = uv.y - y;\n"
          + "    float body = 1.0 - smoothstep(0.0, 0.16 + 0.05 * sin(x * 2.0 + fi + t), abs(d + 0.02));\n"
          + "    float edge = exp(-abs(d) * 70.0);\n"
          + "    float halo = exp(-abs(d) * 9.0) * 0.18;\n"
          + "    vec3 rc = mix(uColorB, uColorC, k);\n"
          + "    col += rc * (body * 0.13 + edge * 0.55 + halo);\n"
          + "  }\n"
          + "  col *= 1.0 - 0.35 * dot(uv - 0.5, uv - 0.5) * 2.0;\n"
          + "  float g = fract(sin(dot(fragCoord, vec2(12.9898, 78.233))) * 43758.5453);\n"
          + "  col += (g - 0.5) / 128.0;\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private static final String FRAG_GALAXY =
      COMMON_HEADER
          + GALAXY_NOISE
          + "void main() {\n"
          + "  vec2 fragCoord = vUv * uResolution;\n"
          + "  vec2 uv = (fragCoord - 0.5 * uResolution.xy) / uResolution.y;\n"
          + "  float t = uTime;\n"
          + "  float ang = t * 0.015;\n"
          + "  vec2 r = mat2(cos(ang), -sin(ang), sin(ang), cos(ang)) * uv;\n"
          + "  float n1 = fbm(r * 2.2 + 3.0);\n"
          + "  float n2 = fbm(r * 4.5 - 9.0 + n1);\n"
          + "  vec3 col = uColorA;\n"
          + "  col += uColorB * pow(n1, 2.4) * 1.5;\n"
          + "  col += uColorC * pow(n2, 3.0) * 1.2;\n"
          + "  col *= 1.0 - 0.9 * smoothstep(0.2, 1.1, length(uv));\n"
          + "  for (int i = 0; i < 3; i++) {\n"
          + "    float fi = float(i);\n"
          + "    float s = 9.0 + fi * 11.0;\n"
          + "    vec2 p = uv * s + vec2(t * 0.05 * (fi + 1.0), t * 0.02) + fi * 17.3;\n"
          + "    vec2 id = floor(p);\n"
          + "    vec2 f = fract(p) - 0.5;\n"
          + "    float h = hash21(id);\n"
          + "    vec2 off = (vec2(hash21(id + 1.3), hash21(id + 7.7)) - 0.5) * 0.6;\n"
          + "    float d = length(f - off);\n"
          + "    float size = 0.04 + 0.06 * hash21(id + 5.1);\n"
          + "    float star = (1.0 - smoothstep(0.0, size, d)) * step(0.80, h);\n"
          + "    float glow = exp(-d * 14.0) * 0.25 * step(0.80, h);\n"
          + "    float tw = 0.65 + 0.35 * sin(t * (1.0 + 3.0 * h) + h * 60.0);\n"
          + "    vec3 sc = mix(vec3(1.0), mix(uColorB, uColorC, hash21(id + 3.1)), 0.45);\n"
          + "    col += sc * (star + glow) * tw * (0.55 + 0.25 * fi);\n"
          + "  }\n"
          + "  float g = fract(sin(dot(fragCoord, vec2(12.9898, 78.233))) * 43758.5453);\n"
          + "  col += (g - 0.5) / 128.0;\n"
          + "  gl_FragColor = vec4(col, 1.0);\n"
          + "}\n";

  private GlslEffects() {}

  /** Returns the vertex shader shared by every effect. */
  @NonNull
  public static String vertexShader() {
    return VERTEX_SHADER;
  }

  /**
   * Returns the fragment shader for a named effect. Unknown names fall back to {@link #LIQUID} so
   * callers never have to guard against a null shader.
   */
  @NonNull
  public static String fragmentShader(@Nullable String name) {
    String key = name == null ? "" : name.toLowerCase(Locale.US);
    switch (key) {
      case SILK:
        return FRAG_SILK;
      case GALAXY:
        return FRAG_GALAXY;
      case LIQUID:
      default:
        return FRAG_LIQUID;
    }
  }
}
