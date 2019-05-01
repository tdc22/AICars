package game;

import broadphase.DynamicAABBTree2;
import display.DisplayMode;
import display.GLDisplay;
import display.PixelFormat;
import display.VideoSettings;
import gui.Font;
import input.Input;
import input.InputEvent;
import input.KeyInput;
import integration.VerletIntegration;
import loader.FontLoader;
import loader.ShaderLoader;
import manifold.MultiPointManifoldManager2;
import math.VecMath;
import narrowphase.EPA2;
import narrowphase.GJK2;
import narrowphase.SupportRaycast2;
import objects.Car;
import objects.Wall;
import physics.PhysicsDebug2;
import physics.PhysicsSpace2;
import positionalcorrection.ProjectionCorrection;
import resolution.ImpulseResolution;
import shader.Shader;
import shape2d.Circle;
import sound.NullSoundEnvironment;
import space.PhysicsProfiler;
import space.SimplePhysicsProfiler;
import utils.Debugger;
import utils.GameProfiler;
import utils.Profiler;
import utils.SimpleGameProfiler;
import vector.Vector2f;

public class Game extends StandardGame {
	PhysicsSpace2 space;
	int tempdelta = 0;
	Debugger debugger;
	Shader defaultshader;
	Profiler profiler;
	PhysicsDebug2 physicsdebug;

	Car car;
	InputEvent up, down, left, right;

	@Override
	public void init() {
		initDisplay(new GLDisplay(), new DisplayMode(1280, 720, "AICars", false), new PixelFormat(),
				new VideoSettings(1920, 1080), new NullSoundEnvironment());

		int defaultShaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		Shader defaultshader3 = new Shader(defaultShaderID);
		addShader(defaultshader3);
		defaultshader = new Shader(defaultShaderID);
		addShader2d(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultShaderID);
		addShaderInterface(defaultshaderInterface);

		space = new PhysicsSpace2(new VerletIntegration(), new DynamicAABBTree2(), new GJK2(new EPA2()),
				new SupportRaycast2(), new ImpulseResolution(), new ProjectionCorrection(1),
				new MultiPointManifoldManager2());

		Font font = FontLoader.loadFont("res/fonts/DejaVuSans.ttf");
		debugger = new Debugger(inputs, defaultshader3, defaultshaderInterface, font, cam);
		physicsdebug = new PhysicsDebug2(inputs, defaultshader, font, space);
		GameProfiler gp = new SimpleGameProfiler();
		setProfiler(gp);
		PhysicsProfiler pp = new SimplePhysicsProfiler();
		space.setProfiler(pp);
		profiler = new Profiler(this, inputs, font, gp, pp);

		up = new InputEvent("Up", new Input(Input.KEYBOARD_EVENT, "Up", KeyInput.KEY_DOWN),
				new Input(Input.KEYBOARD_EVENT, "W", KeyInput.KEY_DOWN));
		down = new InputEvent("Down", new Input(Input.KEYBOARD_EVENT, "Down", KeyInput.KEY_DOWN),
				new Input(Input.KEYBOARD_EVENT, "S", KeyInput.KEY_DOWN));
		left = new InputEvent("Left", new Input(Input.KEYBOARD_EVENT, "Left", KeyInput.KEY_DOWN),
				new Input(Input.KEYBOARD_EVENT, "A", KeyInput.KEY_DOWN));
		right = new InputEvent("Right", new Input(Input.KEYBOARD_EVENT, "Right", KeyInput.KEY_DOWN),
				new Input(Input.KEYBOARD_EVENT, "D", KeyInput.KEY_DOWN));

		inputs.addEvent(up);
		inputs.addEvent(down);
		inputs.addEvent(left);
		inputs.addEvent(right);

		initTrackData();
		car = new Car(600, 200);
		space.addRigidBody(car, car.getBody());
		defaultshader.addObject(car);
	}

	private void addWall(Vector2f a, Vector2f b, Vector2f c, Vector2f d) {
		Wall w = new Wall(a, b, c, d);
		space.addRigidBody(w, w.getBody());
		defaultshader.addObject(w);
	}

	private void initTrackData() {
		initTrack(new Vector2f[] { new Vector2f(600, 220), new Vector2f(1000, 220), new Vector2f(1600, 300),
				new Vector2f(1700, 600), new Vector2f(1600, 900), new Vector2f(1300, 900), new Vector2f(1300, 700),
				new Vector2f(1300, 600), new Vector2f(1200, 500), new Vector2f(1100, 500), new Vector2f(500, 900),
				new Vector2f(200, 700), new Vector2f(400, 400) },
				new float[] { 120, 120, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 });
	}

	private void initTrack(Vector2f[] trackpoints, float[] trackwidths) {
		for (Vector2f v : trackpoints)
			defaultshader.addObject(new Circle(v.x, v.y, 10, 36));

		Vector2f plast = trackpoints[trackpoints.length - 1];
		Vector2f pb = trackpoints[0];
		Vector2f pnext = trackpoints[1];
		for (int i = 0; i < trackpoints.length; i++) {
			Vector2f pa = pb;
			float wa = trackwidths[i];
			pb = pnext;
			float wb;
			if (i < trackpoints.length - 1) {
				wb = trackwidths[i + 1];
			} else {
				wb = trackwidths[0];
			}
			pnext = trackpoints[(i + 2) % trackpoints.length];

			Vector2f trackvec = VecMath.subtraction(pb, pa);
			Vector2f tracknormal = new Vector2f(trackvec.y, -trackvec.x);
			tracknormal.normalize();
			Vector2f normalA = VecMath.subtraction(pb, plast);
			normalA.set(normalA.y, -normalA.x);
			normalA.normalize();
			Vector2f normalB = VecMath.subtraction(pnext, pa);
			normalB.set(normalB.y, -normalB.x);
			normalB.normalize();

			Vector2f a1 = VecMath.scale(normalA, wa);
			Vector2f b1 = VecMath.scale(normalA, wa + 10f);
			Vector2f c1 = VecMath.scale(normalB, wb + 10f);
			Vector2f d1 = VecMath.scale(normalB, wb);

			a1.translate(pa);
			b1.translate(pa);
			c1.translate(pb);
			d1.translate(pb);

			addWall(a1, d1, c1, b1);

			Vector2f a2 = VecMath.scale(normalA, -wa);
			Vector2f b2 = VecMath.scale(normalA, -wa + 10f);
			Vector2f c2 = VecMath.scale(normalB, -wb + 10f);
			Vector2f d2 = VecMath.scale(normalB, -wb);

			a2.translate(pa);
			b2.translate(pa);
			c2.translate(pb);
			d2.translate(pb);

			addWall(a2, d2, c2, b2);

			plast = pa;
		}
	}

	@Override
	public void render() {

	}

	@Override
	public void render2d() {
		debugger.begin();
		render2dLayer();
	}

	@Override
	public void renderInterface() {
		renderInterfaceLayer();
		debugger.end();
	}

	@Override
	public void update(int delta) {
		car.update();
		if (left.isActive()) {
			car.steerLeft();
		}
		if (right.isActive()) {
			car.steerRight();
		}
		if (up.isActive()) {
			car.accelerate();
		}
		if (down.isActive()) {
			car.brake();
		}

		debugger.update(fps, 0, 0);
		space.update(delta);
		physicsdebug.update();
		profiler.update(delta);
	}
}
