/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.material.property;

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.texture.SpriteInfoTexture;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class MaterialTextureState {
	public final int index;
	public final Identifier id;

	private AbstractTexture texture;
	private boolean isAtlas;
	private SpriteInfoTexture atlasInfo;

	private MaterialTextureState(int index, Identifier id) {
		this.index = index;
		this.id = id;
	}

	private void retreiveTexture() {
		if (texture == null) {
			final TextureManager tm = MinecraftClient.getInstance().getTextureManager();
			// forces registration
			tm.bindTexture(id);
			texture = tm.getTexture(id);
			isAtlas = texture != null && texture instanceof SpriteAtlasTexture;

			if (isAtlas) {
				atlasInfo = SpriteInfoTexture.getOrCreate(id);
			} else {
				atlasInfo = null;
			}
		}
	}

	public AbstractTexture texture() {
		retreiveTexture();
		return texture;
	}

	public boolean isAtlas() {
		retreiveTexture();
		return isAtlas;
	}

	public SpriteInfoTexture atlasInfo() {
		retreiveTexture();
		return atlasInfo;
	}

	public void enable(boolean bilinear) {
		if (activeState == this) {
			if (bilinear != activeIsBilinearFilter) {
				final AbstractTexture tex = texture();
				tex.bindTexture();
				tex.setFilter(bilinear, true);
				activeIsBilinearFilter = bilinear;
			}
		} else {
			if (this == MaterialTextureState.NO_TEXTURE) {
				RenderSystem.disableTexture();
			} else {
				RenderSystem.enableTexture();
				final AbstractTexture tex = texture();
				tex.bindTexture();
				tex.setFilter(bilinear, true);

				if (isAtlas()) {
					atlasInfo().enable();
				}

				activeIsBilinearFilter = bilinear;
				activeState = this;
			}
		}
	}

	public static final int MAX_TEXTURE_STATES = 1024;
	private static int nextIndex = 1;
	private static final MaterialTextureState[] STATES = new MaterialTextureState[MAX_TEXTURE_STATES];
	private static final Object2ObjectOpenHashMap<Identifier, MaterialTextureState> MAP = new Object2ObjectOpenHashMap<>(256, Hash.VERY_FAST_LOAD_FACTOR);

	public static final MaterialTextureState NO_TEXTURE = new MaterialTextureState(0, TextureManager.MISSING_IDENTIFIER) {
		@Override
		public void enable(boolean bilinear) {
			if (activeState != this) {
				RenderSystem.disableTexture();
				activeState = this;
			}
		}
	};

	private static MaterialTextureState activeState = NO_TEXTURE;
	private static boolean activeIsBilinearFilter = false;

	public static void disable() {
		if (activeState != null) {
			RenderSystem.enableTexture();
			activeState = null;
		}
	}

	static {
		STATES[0] = NO_TEXTURE;
		MAP.defaultReturnValue(NO_TEXTURE);
	}

	public static MaterialTextureState fromIndex(int index) {
		return STATES[index];
	}

	// PERF: use cow or other method to avoid synch
	public synchronized static MaterialTextureState fromId(Identifier id) {
		MaterialTextureState state = MAP.get(id);

		if (state == NO_TEXTURE) {
			final int index = nextIndex++;
			state = new MaterialTextureState(index, id);
			MAP.put(id, state);
			STATES[index] = state;
		}

		return state;
	}

	public static void reload() {
		MAP.values().forEach(t -> {
			t.texture = null;
		});
	}
}
