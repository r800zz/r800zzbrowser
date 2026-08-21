/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef VRBROWSER_LAYER_BLITTER_DOT_H
#define VRBROWSER_LAYER_BLITTER_DOT_H

#include "VRLayer.h"
#include "vrb/Forward.h"
#include "vrb/MacroUtils.h"
#include "vrb/ResourceGL.h"

#include <memory>
#include <vector> //r800zz
#include <cstdint> //r800zz

namespace crow {

class LayerBlitter;
typedef std::shared_ptr<LayerBlitter> LayerBlitterPtr;

class LayerBlitter : protected vrb::ResourceGL {
public:
  static LayerBlitterPtr Create(vrb::CreationContextPtr& aContext);

  void Draw(const vrb::TextureSurfacePtr& aTexture,
          const VRLayerSurfacePtr& aLayer,
          int aChromaKeyMode, //r800zz
          bool aFlatToEquirectEnabled); //r800zz

  void SetAiSegmentationMask(const std::vector<uint8_t>& aMask, int aWidth, int aHeight); //r800zz

protected:
  struct State;
  LayerBlitter(State& aState, vrb::CreationContextPtr& aContext);
  ~LayerBlitter() = default;

  void InitializeGL() override;
  void ShutdownGL() override;

private:
  State& m;
  LayerBlitter() = delete;
  VRB_NO_DEFAULTS(LayerBlitter)
};

} // namespace crow

#endif // VRBROWSER_LAYER_BLITTER_DOT_H
