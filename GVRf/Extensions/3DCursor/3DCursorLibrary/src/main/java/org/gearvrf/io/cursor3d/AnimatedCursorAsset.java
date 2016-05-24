/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.io.cursor3d;

import android.util.SparseArray;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRHybridObject;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRTexture;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVRRepeatMode;
import org.gearvrf.utility.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Use this {@link CursorAsset} for cases where the {@link Cursor} needs to be animated
 * using a series of textures that define a time series of the animation frames.
 *
 * The class takes the name of the folder in the assets directory that contains all the texture
 * files that help animate the {@link Cursor}.
 *
 * This class in itself only defines texture animations. It is assumed that the object that uses
 * this {@link CursorAsset} already has a {@link GVRMesh} and a {@link GVRMaterial} set.
 */
class AnimatedCursorAsset extends MeshCursorAsset {
    private static final String TAG = AnimatedCursorAsset.class.getSimpleName();
    private List<Future<GVRTexture>> loaderTextures;
    private final static float LOADING_IMAGE_FRAME_ANIMATION_DURATION = 1f;
    private float animationDuration = LOADING_IMAGE_FRAME_ANIMATION_DURATION;
    private final static int LOOP_REPEAT = -1;
    private SparseArray<GVRImageFrameAnimation> animations;
    private final GVRAnimationEngine animationEngine;
    private String[] files;

    AnimatedCursorAsset(GVRContext context, CursorType type, Action action, String[] files) {
        this(context, type, action, files, null);
    }

    AnimatedCursorAsset(GVRContext context, CursorType type, Action action, String[] files,
                        String mesh) {
        super(context, type, action, mesh, files[0]);
        this.files = files;
        animations = new SparseArray<GVRImageFrameAnimation>();
        animationEngine = context.getAnimationEngine();
    }

    @Override
    void set(CursorSceneObject sceneObject) {
        super.set(sceneObject);

        int key = sceneObject.getId();
        GVRImageFrameAnimation animation = animations.get(key);
        if (animation == null) {
            GVRRenderData renderData = renderDataArray.get(key);
            if (renderData == null) {
                Log.e(TAG, "Render data not found, should not happen");
                return;
            }

            GVRMaterial loadingMaterial = renderData.getMaterial();
            loadingMaterial.setMainTexture(loaderTextures.get(0));
            animation = new GVRImageFrameAnimation(loadingMaterial,
                    animationDuration, loaderTextures);
            //Usual animations have a repeat behavior
            animation.setRepeatMode(GVRRepeatMode.REPEATED);
            animation.setRepeatCount(LOOP_REPEAT);
            animations.append(key, animation);
        }

        animationEngine.start(animation).setOnFinish(null);
    }

    @Override
    void reset(CursorSceneObject sceneObject) {
        int key = sceneObject.getId();
        GVRImageFrameAnimation animation = animations.get(key);
        if (animation == null) {
            //nothing to do
            Log.d(TAG, "Animation is finished return, should not happen ");
            super.reset(sceneObject);
            return;
        }

        if (animation.isFinished() == false) {
            if (animation.getRepeatCount() == LOOP_REPEAT) {
                animation.setRepeatCount(0);
                animation.setRepeatMode(GVRRepeatMode.ONCE);
            }
            animationEngine.stop(animation);
        }
        animations.remove(key);

        super.reset(sceneObject);
    }

    @Override
    void load(CursorSceneObject sceneObject) {
        super.load(sceneObject);

        try {
            loaderTextures = getFutureTextureFromAssets(context);
        } catch (IOException e) {
            Log.e(TAG, "Error loading textures", e);
        }
    }

    @Override
    void unload(CursorSceneObject sceneObject) {
        super.unload(sceneObject);
        animations.remove(sceneObject.getId());

        // check if there are cursors still using the textures
        if (renderDataArray.size() == 0) {
            loaderTextures.clear();
        }
    }

    private List<Future<GVRTexture>> getFutureTextureFromAssets(final GVRContext gvrContext)
            throws IOException {
        int numFiles = files.length;
        List<Future<GVRTexture>> listTextures = new ArrayList<Future<GVRTexture>>(
                numFiles);
        for (int fileIdx = 0; fileIdx < numFiles; fileIdx++) {
            Future<GVRTexture> gvrBitmapTexture;
            try {
                gvrBitmapTexture = gvrContext.loadFutureTexture(
                        new GVRAndroidResource(gvrContext, files[fileIdx]));
                listTextures.add(fileIdx, gvrBitmapTexture);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File " + files[fileIdx] + " not found ", e);
            }
        }

        return listTextures;
    }

    void setAnimationDuration(float duration) {
        animationDuration = duration;
    }

    float getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Implements texture update animation.
     */
    private static class GVRImageFrameAnimation extends GVRAnimation {
        private final List<Future<GVRTexture>> animationTextures;
        private int lastFileIndex = -1;

        /**
         * @param material             {@link GVRMaterial} to animate
         * @param duration             The animation duration, in seconds.
         * @param texturesForAnimation arrayList of GVRTexture used during animation
         */

        private GVRImageFrameAnimation(GVRMaterial material, float duration,
                                       final List<Future<GVRTexture>> texturesForAnimation) {
            super(material, duration);
            animationTextures = texturesForAnimation;
        }

        @Override
        protected void animate(GVRHybridObject target, float ratio) {
            final int size = animationTextures.size();
            final int fileIndex = (int) (ratio * size);

            if (lastFileIndex == fileIndex || fileIndex == size) {
                return;
            }

            lastFileIndex = fileIndex;

            GVRMaterial material = (GVRMaterial) target;
            material.setMainTexture(animationTextures.get(fileIndex));
        }
    }
}