// Copyright 2015 Samsung Electronics Co., LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable

precision mediump float;
layout ( set = 0, binding = 4 )uniform sampler2D u_texture;
layout ( location = 0 ) in vec2 diffuse_coord;
layout ( location = 0 ) out vec4 outColor;

void main() {
  vec4 tex = texture(u_texture, diffuse_coord);
  tex = vec4(1.0-tex.x,1.0-tex.y,1.0-tex.z,1.0-tex.w);
  outColor = tex;
}
