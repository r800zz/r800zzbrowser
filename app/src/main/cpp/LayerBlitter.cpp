/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "LayerBlitter.h"

#include <chrono>

#include "vrb/ConcreteClass.h"
#include "vrb/private/ResourceGLState.h"
#include "vrb/GLError.h"
#include "vrb/ShaderUtil.h"
#include "vrb/TextureSurface.h"
#include "vrb/gl.h"
#include "VRBrowser.h" //r800zz
#include <vector> //r800zz

namespace {

const char* sVertexShader = R"SHADER(
attribute vec4 a_position;
attribute vec2 a_uv;
varying vec2 v_uv;

void main() {
  v_uv = a_uv;
  gl_Position = a_position;
}
)SHADER";

constexpr GLsizei kAutoChromaSampleSize = 20;
constexpr std::chrono::milliseconds kAutoChromaSampleInterval(1000);
constexpr float kAutoChromaSimilarityThreshold = 0.035f;
constexpr float kAutoChromaLockedColorThreshold = 0.035f; //r800zz Squared RGB distance.
constexpr int kAutoChromaMinimumMatchingSamples = 4;
constexpr int kAutoChromaStartHitCount = 2; //r800zz
constexpr int kAutoChromaStopMissCount = 5; //r800zz
constexpr GLsizei kAiSegmentationSampleSize = 256; //r800zz
//constexpr GLsizei kAiSegmentationSampleSize = 512; //r800zz
//constexpr GLsizei kAiSegmentationSampleSize = 1024; //r800zz
constexpr bool kAiSegmentationUseFullSize = false; //r800zz Set true to restore full-size AI input.
constexpr std::chrono::milliseconds kAiSegmentationSampleInterval(100); //r800zz

const char* sAutoChromaFragmentShader = R"SHADER(
#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES u_texture0;
varying vec2 v_uv;

void main() {
  gl_FragColor = texture2D(u_texture0, v_uv);
}
)SHADER";

#if defined(PICOXR)
const char* sFragmentShader = R"SHADER(
#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES u_texture0;
uniform float u_flipY; //r800zz
uniform float u_chromaKeyEnabled;
uniform float u_flatToEquirectEnabled; //r800zz
uniform float u_autoChromaEnabled; //r800zz
uniform vec3 u_autoChromaKeyColor; //r800zz
uniform float u_blackChromaEnabled; //r800zz
uniform float u_whiteChromaEnabled; //r800zz
uniform float u_skyBlueChromaEnabled; //r800zz
uniform sampler2D u_aiMaskTexture; //r800zz
uniform float u_aiPassthroughEnabled; //r800zz

varying vec2 v_uv;

void main() {
  vec2 sampleUV = v_uv; //r800zz
  vec4 color = vec4(0.0); //r800zz

  if (u_flatToEquirectEnabled > 0.5) { //r800zz
    const float PI = 3.14159265358979323846; //r800zz
    const float hFov = 120.0 * PI / 180.0; //r800zz
    const float vFov = 120.0 * PI / 180.0; //r800zz

    float yaw = (v_uv.x - 0.5) * PI; //r800zz
    float pitch = (0.5 - v_uv.y) * PI; //r800zz

    vec3 dir; //r800zz
    dir.x = sin(yaw) * cos(pitch); //r800zz
    dir.y = sin(pitch); //r800zz
    dir.z = cos(yaw) * cos(pitch); //r800zz

    if (dir.z > 0.0) { //r800zz
      float px = dir.x / dir.z; //r800zz
      float py = dir.y / dir.z; //r800zz

      sampleUV.x = 0.5 + (px / tan(hFov * 0.5)) * 0.5; //r800zz
      sampleUV.y = 0.5 - (py / tan(vFov * 0.5)) * 0.5; //r800zz

      if (sampleUV.x >= 0.0 && sampleUV.x <= 1.0 &&
          sampleUV.y >= 0.0 && sampleUV.y <= 1.0) { //r800zz
        color = texture2D(u_texture0, sampleUV); //r800zz
      } //r800zz
    } //r800zz
  } else { //r800zz
    color = texture2D(u_texture0, v_uv); //r800zz
  } //r800zz

//
  if (u_aiPassthroughEnabled > 0.5) { //r800zz
    vec2 aiUV = vec2(sampleUV.x, 1.0 - sampleUV.y); //r800zz
    float personMask = texture2D(u_aiMaskTexture, aiUV).r; //r800zz
  
    //float personAlpha = personMask; //r800zz
    float personAlpha = personMask > 0.01 ? 1.0 : 0.0; //r800zz
  
    color.a *= personAlpha; //r800zz
    color.rgb *= personAlpha; //r800zz
  
    if (color.a < 0.01) { //r800zz
      color = vec4(0.0); //r800zz
    } //r800zz
  } //r800zz
//

  if (u_chromaKeyEnabled > 0.5) {
    if (u_autoChromaEnabled > 0.5) {
      float distanceFromKey = distance(color.rgb, u_autoChromaKeyColor);
      float keyAmount = 1.0 - smoothstep(0.10, 0.25, distanceFromKey);

      color.a *= 1.0 - keyAmount;
      color.rgb *= color.a;

      if (color.a < 0.01) {
        color = vec4(0.0);
      }
//
    } else if (u_blackChromaEnabled > 0.5) { //r800zz
      float brightness = max(color.r, max(color.g, color.b)); //r800zz
      float keyAmount = 1.0 - smoothstep(0.02, 0.20, brightness); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
    } else if (u_whiteChromaEnabled > 0.5) { //r800zz
      float whiteness = min(color.r, min(color.g, color.b)); //r800zz
      float keyAmount = smoothstep(0.75, 0.95, whiteness); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
//
    } else if (u_skyBlueChromaEnabled > 0.5) { //r800zz
      vec3 skyBlue = vec3(170.0 / 255.0, 211.0 / 255.0, 230.0 / 255.0); //r800zz
      float distanceFromKey = distance(color.rgb, skyBlue); //r800zz
      float keyAmount = 1.0 - smoothstep(0.10, 0.25, distanceFromKey); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
    } else {
      float maxRB = max(color.r, color.b);

      float greenScore =
          min(color.g - color.b,
              color.g - color.r * 1.05);

      float keyAmount = smoothstep(
          0.06,
          0.20,
          greenScore
      );

      // Strong green becomes fully transparent.
      if (color.g > 0.30 &&
          color.g > color.r * 1.30 &&
          color.g > color.b * 1.30) {
        keyAmount = max(keyAmount, 1.0);
      }

      // Remove green spill before alpha feathering.
      color.g = min(color.g, maxRB);

      color.a *= 1.0 - keyAmount;
      color.rgb *= color.a;

      if (color.a < 0.01) {
        color = vec4(0.0);
      }
    }
  }

  gl_FragColor = color;
}
)SHADER";
#else
const char* sFragmentShader = R"SHADER(
#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES u_texture0;
uniform float u_flipY; //r800zz
uniform float u_chromaKeyEnabled;
uniform float u_flatToEquirectEnabled; //r800zz
uniform float u_autoChromaEnabled; //r800zz
uniform vec3 u_autoChromaKeyColor; //r800zz
uniform float u_blackChromaEnabled; //r800zz
uniform float u_whiteChromaEnabled; //r800zz
uniform float u_skyBlueChromaEnabled; //r800zz
uniform sampler2D u_aiMaskTexture; //r800zz
uniform float u_aiPassthroughEnabled; //r800zz

varying vec2 v_uv;

void main() {
  //vec4 color = texture2D(u_texture0, v_uv);
//
vec2 sampleUV = v_uv; //r800zz
vec4 color = vec4(0.0); //r800zz

if (u_flatToEquirectEnabled > 0.5) { //r800zz
  const float PI = 3.14159265358979323846; //r800zz
  const float hFov = 120.0 * PI / 180.0; //r800zz
  const float vFov = 120.0 * PI / 180.0; //r800zz

  float yaw = (v_uv.x - 0.5) * PI; //r800zz
  float pitch = (0.5 - v_uv.y) * PI; //r800zz

  vec3 dir; //r800zz
  dir.x = sin(yaw) * cos(pitch); //r800zz
  dir.y = sin(pitch); //r800zz
  dir.z = cos(yaw) * cos(pitch); //r800zz

  if (dir.z > 0.0) { //r800zz
    float px = dir.x / dir.z; //r800zz
    float py = dir.y / dir.z; //r800zz

    sampleUV.x = 0.5 + (px / tan(hFov * 0.5)) * 0.5; //r800zz
    sampleUV.y = 0.5 - (py / tan(vFov * 0.5)) * 0.5; //r800zz

    if (sampleUV.x >= 0.0 && sampleUV.x <= 1.0 &&
        sampleUV.y >= 0.0 && sampleUV.y <= 1.0) { //r800zz
if (u_flipY > 0.5) {
  sampleUV.y = 1.0 - sampleUV.y; //r800zz Flip external texture vertically on Oculus FBO path.
}
      color = texture2D(u_texture0, sampleUV); //r800zz
    } //r800zz
  } //r800zz
} else { //r800zz
vec2 normalUV = v_uv; //r800zz
if (u_flipY > 0.5) {
  normalUV.y = 1.0 - normalUV.y; //r800zz Flip external texture vertically on Oculus FBO path.
}
  color = texture2D(u_texture0, normalUV); //r800zz
} //r800zz
//

//
if (u_aiPassthroughEnabled > 0.5) { //r800zz
  vec2 aiUV = vec2(sampleUV.x, 1.0 - sampleUV.y); //r800zz
  float personMask = texture2D(u_aiMaskTexture, aiUV).r; //r800zz

  float personAlpha = personMask; //r800zz

  color.a *= personAlpha; //r800zz
  color.rgb *= personAlpha; //r800zz

  if (color.a < 0.01) { //r800zz
    color = vec4(0.0); //r800zz
  } //r800zz
} //r800zz
//

if (u_chromaKeyEnabled > 0.5) {
  if (u_autoChromaEnabled > 0.5) {
    float distanceFromKey = distance(color.rgb, u_autoChromaKeyColor);
    float keyAmount = 1.0 - smoothstep(0.10, 0.25, distanceFromKey);

    color.a *= 1.0 - keyAmount;
    color.rgb *= color.a;

    if (color.a < 0.01) {
      color = vec4(0.0);
    }
//
    } else if (u_blackChromaEnabled > 0.5) { //r800zz
      float brightness = max(color.r, max(color.g, color.b)); //r800zz
      float keyAmount = 1.0 - smoothstep(0.02, 0.20, brightness); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
    } else if (u_whiteChromaEnabled > 0.5) { //r800zz
      float whiteness = min(color.r, min(color.g, color.b)); //r800zz
      float keyAmount = smoothstep(0.75, 0.95, whiteness); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
//
  } else if (u_skyBlueChromaEnabled > 0.5) { //r800zz
      vec3 skyBlue = vec3(170.0 / 255.0, 211.0 / 255.0, 230.0 / 255.0); //r800zz
      float distanceFromKey = distance(color.rgb, skyBlue); //r800zz
      float keyAmount = 1.0 - smoothstep(0.10, 0.25, distanceFromKey); //r800zz
    
      color.a *= 1.0 - keyAmount; //r800zz
      color.rgb *= color.a; //r800zz
    
      if (color.a < 0.01) { //r800zz
        color = vec4(0.0); //r800zz
      } //r800zz
  } else {
    float maxRB = max(color.r, color.b);

    float greenScore =
        min(color.g - color.b,
            color.g - color.r * 1.05);

    float keyAmount = smoothstep(
        0.06,
        0.20,
        greenScore
    );

    // Strong green becomes fully transparent.
    if (color.g > 0.30 &&
        color.g > color.r * 1.30 &&
        color.g > color.b * 1.30) {
      keyAmount = max(keyAmount, 1.0);
    }

    // Remove green spill before alpha feathering.
    color.g = min(color.g, maxRB);

    color.a *= 1.0 - keyAmount;
    color.rgb *= color.a;

    if (color.a < 0.01) {
      color = vec4(0.0);
    }
  }
}

  gl_FragColor = color;
}
)SHADER";
#endif

const GLfloat sVertices[] = {
    -1.0f,  1.0f, 0.0f,
    -1.0f, -1.0f, 0.0f,
     1.0f,  1.0f, 0.0f,
     1.0f, -1.0f, 0.0f,
};

const GLfloat sUVs[] = {
    0.0f, 0.0f,
    0.0f, 1.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
};

} // namespace

namespace crow {

struct LayerBlitter::State : public vrb::ResourceGL::State {
  GLuint vertexShader = 0;
  GLuint fragmentShader = 0;
  GLuint program = 0;
  GLint aPosition = -1;
  GLint aUV = -1;
  GLint uTexture0 = -1;
  GLint uChromaKeyEnabled = -1;
  GLint uFlatToEquirectEnabled = -1; //r800zz
  GLint uFlipY = -1; //r800zz
  GLint uAutoChromaEnabled = -1; //r800zz
  GLint uBlackChromaEnabled = -1; //r800zz
  GLint uWhiteChromaEnabled = -1; //r800zz
  GLint uSkyBlueChromaEnabled = -1; //r800zz
  GLint uAiMaskTexture = -1; //r800zz
  GLint uAiPassthroughEnabled = -1; //r800zz
  GLint uAutoChromaKeyColor = -1; //r800zz
  GLuint autoChromaVertexShader = 0;
  GLuint autoChromaFragmentShader = 0;
  GLuint autoChromaProgram = 0;
  GLint autoChromaAPosition = -1;
  GLint autoChromaAUV = -1;
  GLint autoChromaUTexture0 = -1;
  GLuint autoChromaFramebuffer = 0;
  GLuint autoChromaTexture = 0;
  std::chrono::steady_clock::time_point autoChromaLastSampleTime;
  bool autoChromaHasSampleTime = false;
  bool autoChromaKeyValid = false;
  int autoChromaConsecutiveHits = 0; //r800zz
  int autoChromaConsecutiveMisses = 0; //r800zz
  GLfloat autoChromaCandidateColor[3] = {0.0f, 1.0f, 0.0f}; //r800zz Not used for rendering.
  GLfloat autoChromaKeyColor[3] = {0.0f, 1.0f, 0.0f}; //r800zz Locked rendering color.
  GLuint aiSegmentationFramebuffer = 0; //r800zz
  GLuint aiSegmentationTexture = 0; //r800zz
  int aiSegmentationWidth = 0; //r800zz
  int aiSegmentationHeight = 0; //r800zz
  std::chrono::steady_clock::time_point aiSegmentationLastSampleTime; //r800zz
  bool aiSegmentationHasSampleTime = false; //r800zz
  GLuint aiMaskTexture = 0; //r800zz
  int aiMaskWidth = 0; //r800zz
  int aiMaskHeight = 0; //r800zz
  bool aiMaskValid = false; //r800zz
};

LayerBlitterPtr
LayerBlitter::Create(vrb::CreationContextPtr& aContext) {
  return std::make_shared<vrb::ConcreteClass<LayerBlitter, LayerBlitter::State>>(aContext);
}

LayerBlitter::LayerBlitter(State& aState, vrb::CreationContextPtr& aContext)
    : vrb::ResourceGL(aState, aContext)
    , m(aState) {}

void
LayerBlitter::SetAiSegmentationMask(const std::vector<uint8_t>& aMask,
                                    int aWidth,
                                    int aHeight) { //r800zz
  if (aMask.empty() || aWidth <= 0 || aHeight <= 0 ||
      aMask.size() != static_cast<size_t>(aWidth * aHeight)) {
    m.aiMaskValid = false; //r800zz
    return;
  }

  if (!m.aiMaskTexture) {
    VRB_GL_CHECK(glGenTextures(1, &m.aiMaskTexture)); //r800zz
  }

  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, m.aiMaskTexture)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)); //r800zz

  VRB_GL_CHECK(glTexImage2D(
      GL_TEXTURE_2D,
      0,
      GL_LUMINANCE,
      aWidth,
      aHeight,
      0,
      GL_LUMINANCE,
      GL_UNSIGNED_BYTE,
      aMask.data())); //r800zz

  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, 0)); //r800zz

  m.aiMaskWidth = aWidth; //r800zz
  m.aiMaskHeight = aHeight; //r800zz
  m.aiMaskValid = true; //r800zz
}

void
LayerBlitter::Draw(const vrb::TextureSurfacePtr& aTexture,
                   const VRLayerSurfacePtr& aLayer,
                   int aChromaKeyMode, //r800zz
                   bool aFlatToEquirectEnabled) {
  if (!m.program || !aTexture || !aLayer || !aLayer->IsInitialized() ||
      aLayer->GetSurfaceType() != VRLayerSurface::SurfaceType::FBO ||
      aTexture->GetHandle() == 0) {
    return;
  }
  const bool greenChromaEnabled = aChromaKeyMode == 1; //r800zz
  const bool autoChromaEnabled = aChromaKeyMode == 2; //r800zz
  const bool blackChromaEnabled = aChromaKeyMode == 3; //r800zz
  const bool whiteChromaEnabled = aChromaKeyMode == 4; //r800zz
  const bool skyBlueChromaEnabled = aChromaKeyMode == 5; //r800zz
  const bool aiPassthroughEnabled = aChromaKeyMode == 6; //r800zz

  const GLboolean depthEnabled = glIsEnabled(GL_DEPTH_TEST);
  const GLboolean blendEnabled = glIsEnabled(GL_BLEND);
  const GLboolean cullEnabled = glIsEnabled(GL_CULL_FACE);
  GLfloat previousClearColor[4];
  VRB_GL_CHECK(glGetFloatv(GL_COLOR_CLEAR_VALUE, previousClearColor));

  if (aChromaKeyMode != 0 && !autoChromaEnabled) { //r800zz
    m.autoChromaHasSampleTime = false;
    m.autoChromaKeyValid = false;
    m.autoChromaConsecutiveHits = 0; //r800zz
    m.autoChromaConsecutiveMisses = 0; //r800zz
    m.autoChromaCandidateColor[0] = 0.0f;
    m.autoChromaCandidateColor[1] = 1.0f;
    m.autoChromaCandidateColor[2] = 0.0f;
    m.autoChromaKeyColor[0] = 0.0f;
    m.autoChromaKeyColor[1] = 1.0f;
    m.autoChromaKeyColor[2] = 0.0f;
  }

  const auto autoChromaNow = std::chrono::steady_clock::now();
  const bool autoChromaSampleDue =
      !m.autoChromaHasSampleTime ||
      autoChromaNow - m.autoChromaLastSampleTime >= kAutoChromaSampleInterval;

  if (autoChromaEnabled &&
      m.autoChromaProgram && m.autoChromaFramebuffer &&
      autoChromaSampleDue) {
    m.autoChromaLastSampleTime = autoChromaNow;
    m.autoChromaHasSampleTime = true;
    GLint previousFramebuffer = 0;
    GLint previousViewport[4] = {0, 0, 0, 0};
    VRB_GL_CHECK(glGetIntegerv(GL_FRAMEBUFFER_BINDING, &previousFramebuffer));
    VRB_GL_CHECK(glGetIntegerv(GL_VIEWPORT, previousViewport));

    VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, m.autoChromaFramebuffer));
    VRB_GL_CHECK(glViewport(0, 0, kAutoChromaSampleSize, kAutoChromaSampleSize));
    VRB_GL_CHECK(glDisable(GL_DEPTH_TEST));
    VRB_GL_CHECK(glDisable(GL_BLEND));
    VRB_GL_CHECK(glDisable(GL_CULL_FACE));
    VRB_GL_CHECK(glUseProgram(m.autoChromaProgram));
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0));
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, aTexture->GetHandle()));
    VRB_GL_CHECK(glUniform1i(m.autoChromaUTexture0, 0));
    VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.autoChromaAPosition, 3, GL_FLOAT,
                                      GL_FALSE, 0, sVertices));
    VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.autoChromaAPosition));
    VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.autoChromaAUV, 2, GL_FLOAT,
                                      GL_FALSE, 0, sUVs));
    VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.autoChromaAUV));
    VRB_GL_CHECK(glDrawArrays(GL_TRIANGLE_STRIP, 0, 4));

    GLubyte pixels[kAutoChromaSampleSize * kAutoChromaSampleSize * 4];
    VRB_GL_CHECK(glReadPixels(0, 0, kAutoChromaSampleSize,
                              kAutoChromaSampleSize, GL_RGBA,
                              GL_UNSIGNED_BYTE, pixels));

    VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.autoChromaAPosition));
    VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.autoChromaAUV));
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0));
    VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, (GLuint)previousFramebuffer));
    VRB_GL_CHECK(glViewport(previousViewport[0], previousViewport[1],
                            previousViewport[2], previousViewport[3]));

    // Sample the 12 internal intersections of a 5-by-4 grid.
    const GLint samplePoints[12][2] = {
        {4, 5}, {8, 5}, {12, 5}, {16, 5},
        {4, 10}, {8, 10}, {12, 10}, {16, 10},
        {4, 15}, {8, 15}, {12, 15}, {16, 15},
    };
    GLfloat samples[12][3];
    for (int i = 0; i < 12; ++i) {
      const int offset = (samplePoints[i][1] * kAutoChromaSampleSize +
                          samplePoints[i][0]) * 4;
      samples[i][0] = pixels[offset] / 255.0f;
      samples[i][1] = pixels[offset + 1] / 255.0f;
      samples[i][2] = pixels[offset + 2] / 255.0f;
    }

    int bestIndex = 0;
    int bestScore = -1;
    for (int i = 0; i < 12; ++i) {
      int score = 0;
      for (int j = 0; j < 12; ++j) {
        if (i == j) {
          continue;
        }
        const GLfloat dr = samples[i][0] - samples[j][0];
        const GLfloat dg = samples[i][1] - samples[j][1];
        const GLfloat db = samples[i][2] - samples[j][2];
        if (dr * dr + dg * dg + db * db < kAutoChromaSimilarityThreshold) {
          ++score;
        }
      }
      if (score > bestScore) {
        bestScore = score;
        bestIndex = i;
      }
    }

    GLfloat detectedColor[3] = {0.0f, 0.0f, 0.0f};
    int detectedCount = 0;
    for (int i = 0; i < 12; ++i) {
      const GLfloat dr = samples[i][0] - samples[bestIndex][0];
      const GLfloat dg = samples[i][1] - samples[bestIndex][1];
      const GLfloat db = samples[i][2] - samples[bestIndex][2];
      if (dr * dr + dg * dg + db * db < kAutoChromaSimilarityThreshold) {
        detectedColor[0] += samples[i][0];
        detectedColor[1] += samples[i][1];
        detectedColor[2] += samples[i][2];
        ++detectedCount;
      }
    }

    const bool autoChromaHit =
        detectedCount >= kAutoChromaMinimumMatchingSamples;

    if (autoChromaHit) {
      detectedColor[0] /= detectedCount;
      detectedColor[1] /= detectedCount;
      detectedColor[2] /= detectedCount;
    }

    if (m.autoChromaKeyValid) {
      // While active, the rendering key color remains locked. A coherent
      // detected color counts as a hit only when it still matches that color.
      bool lockedColorHit = false;
      if (autoChromaHit) {
        const GLfloat dr = detectedColor[0] - m.autoChromaKeyColor[0];
        const GLfloat dg = detectedColor[1] - m.autoChromaKeyColor[1];
        const GLfloat db = detectedColor[2] - m.autoChromaKeyColor[2];
        lockedColorHit =
            dr * dr + dg * dg + db * db <
            kAutoChromaLockedColorThreshold;
      }

      m.autoChromaConsecutiveHits = 0; //r800zz
      if (lockedColorHit) {
        m.autoChromaConsecutiveMisses = 0; //r800zz
      } else {
        ++m.autoChromaConsecutiveMisses; //r800zz
        if (m.autoChromaConsecutiveMisses >= kAutoChromaStopMissCount) {
          m.autoChromaKeyValid = false;
          m.autoChromaConsecutiveMisses = 0;
        }
      }
    } else if (autoChromaHit) {
      m.autoChromaConsecutiveMisses = 0; //r800zz

      // Require consecutive detections of approximately the same color before
      // copying the candidate color into the locked rendering color.
      if (m.autoChromaConsecutiveHits == 0) {
        m.autoChromaCandidateColor[0] = detectedColor[0];
        m.autoChromaCandidateColor[1] = detectedColor[1];
        m.autoChromaCandidateColor[2] = detectedColor[2];
        m.autoChromaConsecutiveHits = 1;
      } else {
        const GLfloat dr =
            detectedColor[0] - m.autoChromaCandidateColor[0];
        const GLfloat dg =
            detectedColor[1] - m.autoChromaCandidateColor[1];
        const GLfloat db =
            detectedColor[2] - m.autoChromaCandidateColor[2];
        const bool candidateColorHit =
            dr * dr + dg * dg + db * db <
            kAutoChromaLockedColorThreshold;

        if (candidateColorHit) {
          ++m.autoChromaConsecutiveHits; //r800zz
          if (m.autoChromaConsecutiveHits >= kAutoChromaStartHitCount) {
            m.autoChromaKeyColor[0] =
                (m.autoChromaCandidateColor[0] + detectedColor[0]) * 0.5f;
            m.autoChromaKeyColor[1] =
                (m.autoChromaCandidateColor[1] + detectedColor[1]) * 0.5f;
            m.autoChromaKeyColor[2] =
                (m.autoChromaCandidateColor[2] + detectedColor[2]) * 0.5f;
            m.autoChromaKeyValid = true;
            m.autoChromaConsecutiveHits = 0;
          }
        } else {
          // The current detection becomes the first hit of a new candidate.
          m.autoChromaCandidateColor[0] = detectedColor[0];
          m.autoChromaCandidateColor[1] = detectedColor[1];
          m.autoChromaCandidateColor[2] = detectedColor[2];
          m.autoChromaConsecutiveHits = 1;
        }
      }
    } else {
      m.autoChromaConsecutiveHits = 0; //r800zz
      m.autoChromaConsecutiveMisses = 0; //r800zz
    }
  }

//
if (!aiPassthroughEnabled) { //r800zz
  m.aiSegmentationHasSampleTime = false; //r800zz
} //r800zz

//
//const int aiWidth = aLayer->GetWidth(); //r800zz
//const int aiHeight = aLayer->GetHeight(); //r800zz
const int aiWidth = kAiSegmentationSampleSize;  //r800zz MODNet fixed square input
const int aiHeight = kAiSegmentationSampleSize; //r800zz MODNet fixed square input

if (aiPassthroughEnabled &&
    m.aiSegmentationTexture &&
    aiWidth > 0 && aiHeight > 0 &&
    (m.aiSegmentationWidth != aiWidth ||
     m.aiSegmentationHeight != aiHeight)) { //r800zz

  GLint previousTexture = 0; //r800zz
  GLint previousActiveTexture = 0; //r800zz

  VRB_GL_CHECK(glGetIntegerv(
      GL_ACTIVE_TEXTURE, &previousActiveTexture)); //r800zz
  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
  VRB_GL_CHECK(glGetIntegerv(
      GL_TEXTURE_BINDING_2D, &previousTexture)); //r800zz

  VRB_GL_CHECK(glBindTexture(
      GL_TEXTURE_2D, m.aiSegmentationTexture)); //r800zz

  VRB_GL_CHECK(glTexImage2D(
      GL_TEXTURE_2D,
      0,
      GL_RGBA,
      aiWidth,
      aiHeight,
      0,
      GL_RGBA,
      GL_UNSIGNED_BYTE,
      nullptr)); //r800zz

  m.aiSegmentationWidth = aiWidth; //r800zz
  m.aiSegmentationHeight = aiHeight; //r800zz

  VRB_GL_CHECK(glBindTexture(
      GL_TEXTURE_2D, (GLuint)previousTexture)); //r800zz
  VRB_GL_CHECK(glActiveTexture(
      (GLenum)previousActiveTexture)); //r800zz
}
//

const auto aiNow = std::chrono::steady_clock::now(); //r800zz
const bool aiSampleDue =
    !m.aiSegmentationHasSampleTime ||
    aiNow - m.aiSegmentationLastSampleTime >= kAiSegmentationSampleInterval; //r800zz

  if (aiPassthroughEnabled &&
      m.autoChromaProgram &&
      m.aiSegmentationFramebuffer &&
      aiSampleDue) { //r800zz
  
    m.aiSegmentationLastSampleTime = aiNow; //r800zz
    m.aiSegmentationHasSampleTime = true; //r800zz
  
    GLint previousFramebuffer = 0; //r800zz
    GLint previousViewport[4] = {0, 0, 0, 0}; //r800zz
  
    VRB_GL_CHECK(glGetIntegerv(GL_FRAMEBUFFER_BINDING, &previousFramebuffer)); //r800zz
    VRB_GL_CHECK(glGetIntegerv(GL_VIEWPORT, previousViewport)); //r800zz
  
    VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, m.aiSegmentationFramebuffer)); //r800zz
    //VRB_GL_CHECK(glViewport( 0, 0, kAiSegmentationSampleSize, kAiSegmentationSampleSize)); //r800zz

    VRB_GL_CHECK(glViewport( 0, 0, aiWidth, aiHeight)); //r800zz
  
    VRB_GL_CHECK(glDisable(GL_DEPTH_TEST)); //r800zz
    VRB_GL_CHECK(glDisable(GL_BLEND)); //r800zz
    VRB_GL_CHECK(glDisable(GL_CULL_FACE)); //r800zz
  
    VRB_GL_CHECK(glUseProgram(m.autoChromaProgram)); //r800zz
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, aTexture->GetHandle())); //r800zz
    VRB_GL_CHECK(glUniform1i(m.autoChromaUTexture0, 0)); //r800zz
  
    VRB_GL_CHECK(glVertexAttribPointer(
        (GLuint)m.autoChromaAPosition, 3, GL_FLOAT,
        GL_FALSE, 0, sVertices)); //r800zz
    VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.autoChromaAPosition)); //r800zz
  
    VRB_GL_CHECK(glVertexAttribPointer(
        (GLuint)m.autoChromaAUV, 2, GL_FLOAT,
        GL_FALSE, 0, sUVs)); //r800zz
    VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.autoChromaAUV)); //r800zz
  
    VRB_GL_CHECK(glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)); //r800zz
  
    std::vector<GLubyte> pixels( aiWidth * aiHeight * 4); //r800zz
  

VRB_GL_CHECK(glReadPixels(
    0, 0,
    aiWidth,
    aiHeight,
    GL_RGBA,
    GL_UNSIGNED_BYTE,
    pixels.data())); //r800zz

VRBrowser::ProcessSegmentationFrame(
    pixels.data(),
    aiWidth,
    aiHeight); //r800zz
  
    VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.autoChromaAPosition)); //r800zz
    VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.autoChromaAUV)); //r800zz
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)); //r800zz
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE1)); //r800zz
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, 0)); //r800zz
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
  
    VRB_GL_CHECK(glBindFramebuffer(
        GL_FRAMEBUFFER, (GLuint)previousFramebuffer)); //r800zz
    VRB_GL_CHECK(glViewport(
        previousViewport[0],
        previousViewport[1],
        previousViewport[2],
        previousViewport[3])); //r800zz
  }
//

  aLayer->Bind();
  VRB_GL_CHECK(glViewport(0, 0, aLayer->GetWidth(), aLayer->GetHeight()));
  VRB_GL_CHECK(glClearColor(0.0f, 0.0f, 0.0f, 0.0f));
  VRB_GL_CHECK(glClear(GL_COLOR_BUFFER_BIT));

  VRB_GL_CHECK(glDisable(GL_DEPTH_TEST));
  VRB_GL_CHECK(glDisable(GL_BLEND));
  VRB_GL_CHECK(glDisable(GL_CULL_FACE));

  VRB_GL_CHECK(glUseProgram(m.program));
  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0));
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, aTexture->GetHandle()));
  VRB_GL_CHECK(glUniform1i(m.uTexture0, 0));
  VRB_GL_CHECK(glUniform1i(m.uAiMaskTexture, 1)); //r800zz
#if defined(OCULUSVR)
  VRB_GL_CHECK(glUniform1f(m.uFlipY, 1.0f)); //r800zz
#else
  VRB_GL_CHECK(glUniform1f(m.uFlipY, 0.0f)); //r800zz
#endif
  VRB_GL_CHECK(glUniform1f(m.uFlatToEquirectEnabled, aFlatToEquirectEnabled ? 1.0f : 0.0f)); //r800zz
  const bool chromaKeyActive =
    greenChromaEnabled ||
    blackChromaEnabled ||
    whiteChromaEnabled ||
    skyBlueChromaEnabled ||
    (autoChromaEnabled && m.autoChromaKeyValid); //r800zz

  const bool autoChromaActive =
    autoChromaEnabled && m.autoChromaKeyValid; //r800zz
  VRB_GL_CHECK(glUniform1f(m.uChromaKeyEnabled,
                           chromaKeyActive ? 1.0f : 0.0f));
  VRB_GL_CHECK(glUniform1f(m.uAutoChromaEnabled,
                           autoChromaActive ? 1.0f : 0.0f)); //r800zz
  VRB_GL_CHECK(glUniform1f(
    m.uBlackChromaEnabled,
    blackChromaEnabled ? 1.0f : 0.0f)); //r800zz

  VRB_GL_CHECK(glUniform1f(
    m.uWhiteChromaEnabled,
    whiteChromaEnabled ? 1.0f : 0.0f)); //r800zz

  VRB_GL_CHECK(glUniform1f(
    m.uSkyBlueChromaEnabled,
    skyBlueChromaEnabled ? 1.0f : 0.0f)); //r800zz

  const bool aiPassthroughActive =
    aiPassthroughEnabled && m.aiMaskValid; //r800zz

  VRB_GL_CHECK(glUniform1f(
    m.uAiPassthroughEnabled,
    aiPassthroughActive ? 1.0f : 0.0f)); //r800zz

  if (aiPassthroughActive) { //r800zz
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE1)); //r800zz
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, m.aiMaskTexture)); //r800zz
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
  }

  VRB_GL_CHECK(glUniform3f(m.uAutoChromaKeyColor,
                           m.autoChromaKeyColor[0],
                           m.autoChromaKeyColor[1],
                           m.autoChromaKeyColor[2])); //r800zz

  VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.aPosition, 3, GL_FLOAT, GL_FALSE, 0, sVertices));
  VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.aPosition));
  VRB_GL_CHECK(glVertexAttribPointer((GLuint)m.aUV, 2, GL_FLOAT, GL_FALSE, 0, sUVs));
  VRB_GL_CHECK(glEnableVertexAttribArray((GLuint)m.aUV));
  VRB_GL_CHECK(glDrawArrays(GL_TRIANGLE_STRIP, 0, 4));

  VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.aPosition));
  VRB_GL_CHECK(glDisableVertexAttribArray((GLuint)m.aUV));

  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE1)); //r800zz
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, 0)); //r800zz
  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0));

  if (depthEnabled) VRB_GL_CHECK(glEnable(GL_DEPTH_TEST));
  if (blendEnabled) VRB_GL_CHECK(glEnable(GL_BLEND));
  if (cullEnabled) VRB_GL_CHECK(glEnable(GL_CULL_FACE));
  VRB_GL_CHECK(glClearColor(previousClearColor[0], previousClearColor[1],
                           previousClearColor[2], previousClearColor[3]));

  aLayer->Unbind();
}

void
LayerBlitter::InitializeGL() {
  m.vertexShader = vrb::LoadShader(GL_VERTEX_SHADER, sVertexShader);
  m.fragmentShader = vrb::LoadShader(GL_FRAGMENT_SHADER, sFragmentShader);
  if (m.vertexShader && m.fragmentShader) {
    m.program = vrb::CreateProgram(m.vertexShader, m.fragmentShader);
  }
  if (m.program) {
    m.aPosition = vrb::GetAttributeLocation(m.program, "a_position");
    m.aUV = vrb::GetAttributeLocation(m.program, "a_uv");
    m.uTexture0 = vrb::GetUniformLocation(m.program, "u_texture0");
    m.uChromaKeyEnabled = vrb::GetUniformLocation(m.program, "u_chromaKeyEnabled");
    m.uFlipY = vrb::GetUniformLocation(m.program, "u_flipY"); //r800zz
    m.uFlatToEquirectEnabled = vrb::GetUniformLocation(m.program, "u_flatToEquirectEnabled"); //r800zz
    m.uAutoChromaEnabled = vrb::GetUniformLocation(m.program, "u_autoChromaEnabled"); //r800zz
    m.uBlackChromaEnabled = vrb::GetUniformLocation(m.program, "u_blackChromaEnabled"); //r800zz
    m.uWhiteChromaEnabled = vrb::GetUniformLocation(m.program, "u_whiteChromaEnabled"); //r800zz
    m.uSkyBlueChromaEnabled = vrb::GetUniformLocation(m.program, "u_skyBlueChromaEnabled"); //r800zz
    m.uAiMaskTexture = vrb::GetUniformLocation(m.program, "u_aiMaskTexture"); //r800zz
    m.uAiPassthroughEnabled = vrb::GetUniformLocation(m.program, "u_aiPassthroughEnabled"); //r800zz
    m.uAutoChromaKeyColor = vrb::GetUniformLocation(m.program, "u_autoChromaKeyColor"); //r800zz
  }

  m.autoChromaVertexShader = vrb::LoadShader(GL_VERTEX_SHADER, sVertexShader);
  m.autoChromaFragmentShader =
      vrb::LoadShader(GL_FRAGMENT_SHADER, sAutoChromaFragmentShader);
  if (m.autoChromaVertexShader && m.autoChromaFragmentShader) {
    m.autoChromaProgram =
        vrb::CreateProgram(m.autoChromaVertexShader, m.autoChromaFragmentShader);
  }
  if (m.autoChromaProgram) {
    m.autoChromaAPosition =
        vrb::GetAttributeLocation(m.autoChromaProgram, "a_position");
    m.autoChromaAUV = vrb::GetAttributeLocation(m.autoChromaProgram, "a_uv");
    m.autoChromaUTexture0 =
        vrb::GetUniformLocation(m.autoChromaProgram, "u_texture0");

    GLint previousFramebuffer = 0;
    GLint previousTexture = 0;
    GLint previousActiveTexture = 0;
    VRB_GL_CHECK(glGetIntegerv(GL_FRAMEBUFFER_BINDING, &previousFramebuffer));
    VRB_GL_CHECK(glGetIntegerv(GL_ACTIVE_TEXTURE, &previousActiveTexture));
    VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0));
    VRB_GL_CHECK(glGetIntegerv(GL_TEXTURE_BINDING_2D, &previousTexture));

    VRB_GL_CHECK(glGenTextures(1, &m.autoChromaTexture));
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, m.autoChromaTexture));
    VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR));
    VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR));
    VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE));
    VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE));
    VRB_GL_CHECK(glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                              kAutoChromaSampleSize, kAutoChromaSampleSize,
                              0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr));

    VRB_GL_CHECK(glGenFramebuffers(1, &m.autoChromaFramebuffer));
    VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, m.autoChromaFramebuffer));
    VRB_GL_CHECK(glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                        GL_TEXTURE_2D, m.autoChromaTexture, 0));

    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
      VRB_GL_CHECK(glDeleteFramebuffers(1, &m.autoChromaFramebuffer));
      VRB_GL_CHECK(glDeleteTextures(1, &m.autoChromaTexture));
      m.autoChromaFramebuffer = 0;
      m.autoChromaTexture = 0;
    }

    VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, (GLuint)previousFramebuffer));
    VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, (GLuint)previousTexture));
    VRB_GL_CHECK(glActiveTexture((GLenum)previousActiveTexture));
  }

  // Create a separate framebuffer for AI segmentation input. //r800zz
  GLint previousFramebuffer = 0; //r800zz
  GLint previousTexture = 0; //r800zz
  GLint previousActiveTexture = 0; //r800zz
  
  VRB_GL_CHECK(glGetIntegerv(GL_FRAMEBUFFER_BINDING, &previousFramebuffer)); //r800zz
  VRB_GL_CHECK(glGetIntegerv(GL_ACTIVE_TEXTURE, &previousActiveTexture)); //r800zz
  VRB_GL_CHECK(glActiveTexture(GL_TEXTURE0)); //r800zz
  VRB_GL_CHECK(glGetIntegerv(GL_TEXTURE_BINDING_2D, &previousTexture)); //r800zz
  
  VRB_GL_CHECK(glGenTextures(1, &m.aiSegmentationTexture)); //r800zz
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, m.aiSegmentationTexture)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)); //r800zz
  VRB_GL_CHECK(glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)); //r800zz
  
  VRB_GL_CHECK(glTexImage2D(
      GL_TEXTURE_2D, 0, GL_RGBA,
      kAiSegmentationSampleSize, kAiSegmentationSampleSize,
      0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr)); //r800zz
  
  VRB_GL_CHECK(glGenFramebuffers(1, &m.aiSegmentationFramebuffer)); //r800zz
  VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, m.aiSegmentationFramebuffer)); //r800zz
  VRB_GL_CHECK(glFramebufferTexture2D(
      GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
      GL_TEXTURE_2D, m.aiSegmentationTexture, 0)); //r800zz
  
  if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) { //r800zz
    VRB_GL_CHECK(glDeleteFramebuffers(1, &m.aiSegmentationFramebuffer)); //r800zz
    VRB_GL_CHECK(glDeleteTextures(1, &m.aiSegmentationTexture)); //r800zz
    m.aiSegmentationFramebuffer = 0; //r800zz
    m.aiSegmentationTexture = 0; //r800zz
  } //r800zz
  
  VRB_GL_CHECK(glBindFramebuffer(GL_FRAMEBUFFER, (GLuint)previousFramebuffer)); //r800zz
  VRB_GL_CHECK(glBindTexture(GL_TEXTURE_2D, (GLuint)previousTexture)); //r800zz
  VRB_GL_CHECK(glActiveTexture((GLenum)previousActiveTexture)); //r800zz
}

void
LayerBlitter::ShutdownGL() {
  if (m.autoChromaFramebuffer) {
    VRB_GL_CHECK(glDeleteFramebuffers(1, &m.autoChromaFramebuffer));
    m.autoChromaFramebuffer = 0;
  }
  if (m.autoChromaTexture) {
    VRB_GL_CHECK(glDeleteTextures(1, &m.autoChromaTexture));
    m.autoChromaTexture = 0;
  }
  if (m.autoChromaProgram) {
    VRB_GL_CHECK(glDeleteProgram(m.autoChromaProgram));
    m.autoChromaProgram = 0;
  }
  if (m.autoChromaVertexShader) {
    VRB_GL_CHECK(glDeleteShader(m.autoChromaVertexShader));
    m.autoChromaVertexShader = 0;
  }
  if (m.autoChromaFragmentShader) {
    VRB_GL_CHECK(glDeleteShader(m.autoChromaFragmentShader));
    m.autoChromaFragmentShader = 0;
  }

  if (m.program) {
    VRB_GL_CHECK(glDeleteProgram(m.program));
    m.program = 0;
  }
  if (m.vertexShader) {
    VRB_GL_CHECK(glDeleteShader(m.vertexShader));
    m.vertexShader = 0;
  }
  if (m.fragmentShader) {
    VRB_GL_CHECK(glDeleteShader(m.fragmentShader));
    m.fragmentShader = 0;
  }
}

} // namespace crow
