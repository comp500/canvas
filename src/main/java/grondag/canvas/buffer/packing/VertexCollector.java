package grondag.canvas.buffer.packing;

import net.minecraft.client.render.VertexConsumer;

public interface VertexCollector extends VertexConsumer {
	void add(int i);

	void add(float f);

	void add(int[] appendData, int length);
}
