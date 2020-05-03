package cars;

import math.VecMath;
import matrix.Matrix1f;
import matrix.Matrix2;
import objects.Ray2;
import objects.RigidBody2;
import physics.PhysicsShapeCreator;
import quaternion.Complexf;
import shape2d.Quad;
import vector.Vector2f;

public class Car extends Quad {
	RigidBody2 body;
	Vector2f initialdirection, tmpvelocity;
	public Vector2f direction;
	boolean forward;

	public final static float halfwidth = 12;
	public final static float halflength = 33;
	final float speed = 150;
	final float speedscale = speed * speed;
	final float forwarddamping = 0.25f;
	final float backwarddamping = 0.5f;

	public Ray2[] rays;
	public Vector2f[] raystartpoints;

	public Car(float x, float y, int numrays) {
		super(x, y, halflength, halfwidth);
		body = new RigidBody2(PhysicsShapeCreator.create(this));
		body.setMass(1f);
		body.setInertia(new Matrix1f());
		body.setLinearDamping(forwarddamping);
		body.setAngularDamping(2f);
		initialdirection = new Vector2f(1, 0);
		direction = new Vector2f();
		tmpvelocity = new Vector2f();

		rays = new Ray2[numrays];
		raystartpoints = new Vector2f[numrays];
		Complexf rayrotation = new Complexf();
		rayrotation.rotate(360 / (float) numrays);
		Vector2f dir = new Vector2f(1, 0);
		for (int i = 0; i < numrays; i++) {
			rays[i] = new Ray2(new Vector2f(), new Vector2f());
			raystartpoints[i] = body.supportPointLocal(dir);
			raystartpoints[i].scale(0.5f);
			dir.transform(rayrotation);
		}
	}

	public RigidBody2 getBody() {
		return body;
	}

	public void update() {
		direction.set(initialdirection);
		Matrix2 m = body.getMatrix().getSubMatrix2();
		m.transpose();
		direction.transform(m);
		tmpvelocity.set(direction);
		tmpvelocity.scale(speed);
		float veldot = VecMath.dotproduct(body.getLinearVelocity(), tmpvelocity);
		forward = veldot > 0;
		body.getLinearVelocity().set(tmpvelocity);
		body.getLinearVelocity().scale(veldot / speedscale);
	}

	public void accelerate() {
		body.setLinearDamping(forwarddamping);
		body.applyCentralForce(tmpvelocity);
	}

	public void brake() {
		tmpvelocity.negate();
		body.setLinearDamping(backwarddamping);
		body.applyCentralForce(tmpvelocity);
		tmpvelocity.negate();
	}

	public void steerLeft() {
		float factor = forward ? -1 : 1;
		body.applyTorque(factor * (float) Math.sqrt(body.getLinearVelocity().length()) * 15);
	}

	public void steerRight() {
		float factor = forward ? 1 : -1;
		body.applyTorque(factor * (float) Math.sqrt(body.getLinearVelocity().length()) * 15);
	}
}
