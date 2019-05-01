package objects;

import math.VecMath;
import matrix.Matrix1f;
import quaternion.Complexf;
import utils.GLConstants;
import vector.Vector2f;

public class Wall extends ShapedObject2 {
	private Vector2f localA, localB, localC, localD;
	RigidBody2 body;

	public Wall(Vector2f a, Vector2f b, Vector2f c, Vector2f d) {
		Vector2f ACcenter = VecMath.subtraction(c, a);
		ACcenter.scale(0.5f);
		ACcenter.translate(a);
		Vector2f BDcenter = VecMath.subtraction(d, b);
		BDcenter.scale(0.5f);
		BDcenter.translate(b);
		Vector2f center = VecMath.subtraction(BDcenter, ACcenter);
		if (center.length() <= 0.001f) {
			center = ACcenter;
		} else {
			center.scale(0.5f);
			center.translate(ACcenter);
		}
		translateTo(center);
		localA = VecMath.subtraction(a, center);
		localB = VecMath.subtraction(b, center);
		localC = VecMath.subtraction(c, center);
		localD = VecMath.subtraction(d, center);

		addVertex(localA);
		addVertex(localB);
		addVertex(localC);
		addVertex(localD);
		setRenderMode(GLConstants.TRIANGLE_ADJACENCY);
		addQuad(0, 0, 1, 1, 2, 2, 3, 3);
		this.prerender();

		float minX = Math.min(localA.x, Math.min(localB.x, Math.min(localC.x, localD.x)));
		float minY = Math.min(localA.y, Math.min(localB.y, Math.min(localC.y, localD.y)));
		float maxX = Math.max(localA.x, Math.max(localB.x, Math.max(localC.x, localD.x)));
		float maxY = Math.max(localA.y, Math.max(localB.y, Math.max(localC.y, localD.y)));
		body = new RigidBody2(new WallShape(center.x, center.y, minX, minY, maxX, maxY));
	}

	public RigidBody2 getBody() {
		return body;
	}

	private class WallShape extends CollisionShape2 {
		WallShape(float x, float y, float minX, float minY, float maxX, float maxY) {
			super();
			translate(x, y);
			setAABB(new Vector2f(minX, minY), new Vector2f(maxX, maxY));
			supportcalculator = createSupportCalculator(this);
		}

		@Override
		public SupportCalculator<Vector2f> createSupportCalculator(CollisionShape<Vector2f, Complexf, Matrix1f> cs) {
			return new WallSupport();
		}
	}

	private class WallSupport implements SupportCalculator<Vector2f> {
		@Override
		public Vector2f supportPointLocal(Vector2f direction) {
			Vector2f v;
			float dA = VecMath.dotproduct(direction, localA);
			float dB = VecMath.dotproduct(direction, localB);
			if (dA >= dB) {
				float dD = VecMath.dotproduct(direction, localD);
				if (dA >= dD) {
					v = new Vector2f(localA);
				} else {
					float dC = VecMath.dotproduct(direction, localC);
					if (dD >= dC) {
						v = new Vector2f(localD);
					} else {
						v = new Vector2f(localC);
					}
				}
			} else {
				float dC = VecMath.dotproduct(direction, localC);
				if (dB >= dC) {
					v = new Vector2f(localB);
				} else {
					float dD = VecMath.dotproduct(direction, localD);
					if (dC >= dD) {
						v = new Vector2f(localC);
					} else {
						v = new Vector2f(localD);
					}
				}
			}
			return v;
		}

		@Override
		public Vector2f supportPointLocalNegative(Vector2f direction) {
			Vector2f v;
			float dA = VecMath.dotproduct(direction, localA);
			float dB = VecMath.dotproduct(direction, localB);
			if (dA < dB) {
				float dD = VecMath.dotproduct(direction, localD);
				if (dA < dD) {
					v = new Vector2f(localA);
				} else {
					float dC = VecMath.dotproduct(direction, localC);
					if (dD < dC) {
						v = new Vector2f(localD);
					} else {
						v = new Vector2f(localC);
					}
				}
			} else {
				float dC = VecMath.dotproduct(direction, localC);
				if (dB < dC) {
					v = new Vector2f(localB);
				} else {
					float dD = VecMath.dotproduct(direction, localD);
					if (dC < dD) {
						v = new Vector2f(localC);
					} else {
						v = new Vector2f(localD);
					}
				}
			}
			return v;
		}

		@Override
		public boolean isCompound() {
			return false;
		}
	}
}