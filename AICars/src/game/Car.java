package game;

import math.VecMath;
import matrix.Matrix1f;
import matrix.Matrix2;
import objects.RigidBody2;
import physics.PhysicsShapeCreator;
import shape2d.Quad;
import vector.Vector2f;

public class Car extends Quad {
	RigidBody2 body;
	Vector2f initialdirection, direction, tmpvelocity;
	boolean forward;

	public Car(float x, float y) {
		super(x, y, 33, 12);
		body = new RigidBody2(PhysicsShapeCreator.create(this));
		body.setMass(1f);
		body.setInertia(new Matrix1f());
		body.setLinearDamping(0.25f);
		body.setAngularDamping(2f);
		initialdirection = new Vector2f(100, 0);
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
		tmpvelocity.set(direction);
		tmpvelocity.scale(veldot / 10000);
		body.setLinearVelocity(tmpvelocity);
	}
	
	public void accelerate() {
		forward = true;
		body.applyCentralForce(direction);
	}
	
	public void brake() {
		forward = false;
		direction.negate();
		body.applyCentralForce(direction);
		direction.negate();
	}
	
	public void steerLeft() {
		float factor = forward ? -1 : 1;
		body.applyTorque(factor * (float) body.getLinearVelocity().lengthSquared()/500f);
	}
	
	public void steerRight() {
		float factor = forward ? 1 : -1;
		body.applyTorque(factor * (float) body.getLinearVelocity().lengthSquared()/500f);
	}
}
