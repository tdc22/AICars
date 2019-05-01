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
				new VideoSettings(1280, 720), new NullSoundEnvironment());

		Shader defaultshader3 = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		addShader(defaultshader3);
		defaultshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
		addShader2d(defaultshader);
		Shader defaultshaderInterface = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert", "res/shaders/defaultshader.frag"));
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

		car = new Car(100, 120);
		space.addRigidBody(car, car.getBody());
		defaultshader.addObject(car);

		Wall w = new Wall(new Vector2f(300, 200), new Vector2f(300, 400), new Vector2f(320, 400),
				new Vector2f(320, 200));
		space.addRigidBody(w, w.getBody());
		defaultshader.addObject(w);

		initTrackData();
	}

	private void initTrackData() {
		initTrack(new Vector2f[] { new Vector2f(500, 100), new Vector2f(800, 400), new Vector2f(400, 600),
				new Vector2f(200, 500) }, new float[] { 200, 200, 200, 100 }, new float[] { 20, 10, 10, 10 });
	}

	private void initTrack(Vector2f[] trackpoints, float[] trackwidths, float[] runoffwidths) {
		for (int i = 0; i < trackpoints.length; i++) {
			Vector2f pa = trackpoints[i];
			float wa = trackwidths[i];
			float ra = runoffwidths[i];
			Vector2f pb;
			float wb, rb;
			if (i < trackpoints.length - 1) {
				pb = trackpoints[i + 1];
				wb = trackwidths[i + 1];
				rb = runoffwidths[i + 1];
			} else {
				pb = trackpoints[0];
				wb = trackwidths[0];
				rb = runoffwidths[0];
			}

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
