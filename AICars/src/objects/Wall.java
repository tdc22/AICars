package objects;

import gui.Color;
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

		Color col = Color.WHITE;
		this.addVertex(localA, col);
		this.addVertex(localB, col);
		this.addVertex(localC, col);
		this.addVertex(localD, col);
		setRenderMode(GLConstants.TRIANGLE_ADJACENCY);
		addQuad(0, 0, 1, 1, 2, 2, 3, 3);
		this.prerender();

		float minX = Math.min(localA.x, Math.min(localB.x, Math.min(localC.x, localD.x)));
		float minY = Math.min(localA.y, Math.min(localB.y, Math.min(localC.y, localD.y)));
		float maxX = Math.max(localA.x, Math.max(localB.x, Math.max(localC.x, localD.x)));
		float maxY = Math.max(localA.y, Math.max(localB.y, Math.max(localC.y, localD.y)));
		body = new RigidBody2(new WallShape(minX, minY, maxX, maxY));
	}

	public RigidBody2 getBody() {
		return body;
	}

	private class WallShape extends CollisionShape2 {
		WallShape(float minX, float minY, float maxX, float maxY) {
			setAABB(new Vector2f(minX, minY), new Vector2f(maxX, maxY));
		}

		@Override
		public SupportCalculator<Vector2f> createSupportCalculator(CollisionShape<Vector2f, Complexf, Matrix1f> cs) {
			return new WallSupport();
		}
	}

	private class WallSupport implements SupportCalculator<Vector2f> {
		@Override
		public Vector2f supportPointLocal(Vector2f direction) {
			float dA = VecMath.dotproduct(direction, localA);
			float dB = VecMath.dotproduct(direction, localB);
			if (dA > dB) {
				float dD = VecMath.dotproduct(direction, localD);
				if (dA > dD) {
					return localA;
				} else {
					float dC = VecMath.dotproduct(direction, localC);
					if (dD > dC) {
						return localD;
					} else {
						return localC;
					}
				}
			} else {
				float dC = VecMath.dotproduct(direction, localC);
				if (dB > dC) {
					return localB;
				} else {
					float dD = VecMath.dotproduct(direction, localD);
					if (dC > dD) {
						return localC;
					} else {
						return localD;
					}
				}
			}
		}

		@Override
		public Vector2f supportPointLocalNegative(Vector2f direction) {
			float dA = VecMath.dotproduct(direction, localA);
			float dB = VecMath.dotproduct(direction, localB);
			if (dA < dB) {
				float dD = VecMath.dotproduct(direction, localD);
				if (dA < dD) {
					return localA;
				} else {
					float dC = VecMath.dotproduct(direction, localC);
					if (dD < dC) {
						return localD;
					} else {
						return localC;
					}
				}
			} else {
				float dC = VecMath.dotproduct(direction, localC);
				if (dB < dC) {
					return localB;
				} else {
					float dD = VecMath.dotproduct(direction, localD);
					if (dC < dD) {
						return localC;
					} else {
						return localD;
					}
				}
			}
		}

		@Override
		public boolean isCompound() {
			return false;
		}
	}
}