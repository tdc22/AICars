package objects;

import math.VecMath;
import matrix.Matrix1f;
import matrix.Matrix2;
import physics.PhysicsShapeCreator;
import shape2d.Quad;
import vector.Vector2f;

public class Car extends Quad {
	RigidBody2 body;
	Vector2f initialdirection, direction, tmpvelocity;
	boolean forward;

	final float speed = 150;
	final float speedscale = speed * speed;

	public Car(float x, float y) {
		super(x, y, 33, 12);
		body = new RigidBody2(PhysicsShapeCreator.create(this));
		body.setMass(1f);
		body.setInertia(new Matrix1f());
		body.setLinearDamping(0.25f);
		body.setAngularDamping(2f);
		initialdirection = new Vector2f(speed, 0);
		direction = new Vector2f();
		tmpvelocity = new Vector2f();
	}

	public RigidBody2 getBody() {
		return body;
	}

	public void update() {
		direction.set(initialdirection);
		Matrix2 m = body.getMatrix().getSubMatrix2();
		m.transpose();
		direction.transform(m);
		float veldot = VecMath.dotproduct(body.getLinearVelocity(), direction);
		forward = veldot > 0;
		tmpvelocity.set(direction);
		tmpvelocity.scale(veldot / speedscale);
		body.setLinearVelocity(tmpvelocity);
	}

	public void accelerate() {
		body.applyCentralForce(direction);
	}

	public void brake() {
		direction.negate();
		body.applyCentralForce(direction);
		direction.negate();
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
