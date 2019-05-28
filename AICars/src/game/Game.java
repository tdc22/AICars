package game;

import ai.NeuralNetwork;
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
import manifold.RaycastResult;
import math.VecMath;
import narrowphase.EPA2;
import narrowphase.GJK2;
import narrowphase.SupportRaycast2;
import objects.Car;
import objects.Ray2;
import objects.Wall;
import physics.PhysicsDebug2;
import physics.PhysicsSpace2;
import positionalcorrection.ProjectionCorrection;
import quaternion.Complexf;
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
import vector.Vector1f;
import vector.Vector2f;
import vector.Vector4f;

public class Game extends StandardGame {
	PhysicsSpace2 space;
	int tempdelta = 0;
	Debugger debugger;
	Shader defaultshader;
	Profiler profiler;
	PhysicsDebug2 physicsdebug;

	Car car;
	int mode = 1;
	InputEvent up, down, left, right, mode1, mode2, mode3;
	int numRaycasts = 10;
	Circle[] raycastTrackers;
	float[] raycastDistances;
	
	NeuralNetwork nn;
	int nnInputs = numRaycasts + 5;
	float undefinedRayDistance = 0;
	boolean lastLeft = false, lastRight = false, lastUp = false, lastDown = false;

	int trainingtimer = 0;
	int timeBetweenSplits = 100;
	int splitlength = 300;
	int timeline = 0;
	float velocityAfterInterval;
	Vector2f splitPosition = new Vector2f();
	Complexf splitRotation = new Complexf();
	Vector2f splitVelocity = new Vector2f();
	Vector1f splitAngularVelocity = new Vector1f();
	float[] inputsOnSplit;
	float[] expectedOutputs;
	boolean firstIterationOfAlternativeTimeline = true;

	@Override
	public void init() {
		initDisplay(new GLDisplay(), new DisplayMode(1280, 720, "AICars", true), new PixelFormat(),
				new VideoSettings(1920, 1080), new NullSoundEnvironment());

		int defaultShaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		Shader defaultshader3 = new Shader(defaultShaderID);
		addShader(defaultshader3);
		defaultshader = new Shader(defaultShaderID);
		addShader2d(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultShaderID);
		addShaderInterface(defaultshaderInterface);
		Shader raycasttrackershader = new Shader(ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert",
				"res/shaders/colorshader.frag"));
		raycasttrackershader.addArgument("u_color", new Vector4f(1, 0, 0, 1));
		addShader2d(raycasttrackershader);

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
		mode1 = new InputEvent("Mode1", new Input(Input.KEYBOARD_EVENT, "1", KeyInput.KEY_PRESSED));
		mode2 = new InputEvent("Mode2", new Input(Input.KEYBOARD_EVENT, "2", KeyInput.KEY_PRESSED));
		mode3 = new InputEvent("Mode3", new Input(Input.KEYBOARD_EVENT, "3", KeyInput.KEY_PRESSED));

		inputs.addEvent(up);
		inputs.addEvent(down);
		inputs.addEvent(left);
		inputs.addEvent(right);
		inputs.addEvent(mode1);
		inputs.addEvent(mode2);
		inputs.addEvent(mode3);

		initTrackData();
		car = new Car(600, 200, numRaycasts);
		space.addRigidBody(car, car.getBody());
		defaultshader.addObject(car);
		
		raycastTrackers = new Circle[numRaycasts];
		raycastDistances = new float[numRaycasts];
		for(int i = 0; i < numRaycasts; i++) {
			Circle c = new Circle(0, 0, 5, 18);
			raycasttrackershader.addObject(c);
			raycastTrackers[i] = c;
			raycastDistances[i] = undefinedRayDistance;
		}
		rayrotation.rotate(360 / (float) numRaycasts);
		
		int nnInputcount = 5 + numRaycasts;
		int nnOutputcount = 4;
		nn = new NeuralNetwork(new int[] {nnInputcount, 100, 100, 100, nnOutputcount});
		inputsOnSplit = new float[nnInputcount];
		expectedOutputs = new float[nnOutputcount];
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
	
	private Complexf rayrotation = new Complexf();
	private Vector2f lastdirection = new Vector2f();
	private Vector2f raystart = new Vector2f();
	private void singleRaycast(Car c, int i) {
		Ray2 ray = c.rays[i];
		ray.getDirection().set(lastdirection);
		raystart.set(c.raystartpoints[i]);
		raystart.transform(c.getRotation());
		raystart.translate(c.getTranslation());
		ray.getPosition().set(raystart);
		RaycastResult<Vector2f> rr = space.raycast(ray);
		if(rr != null) {
			raycastTrackers[i].translateTo(rr.getHitPosition());
			raycastDistances[i] = rr.getHitDistance();
		}
		else {
			raycastTrackers[i].translateTo(-100, -100);
			raycastDistances[i] = undefinedRayDistance;
		}
	}
	private void doRaycasts(Car c) {
		lastdirection.set(c.direction);
		singleRaycast(c, 0);
		for(int i = 1; i < numRaycasts; i++) {
			lastdirection.transform(rayrotation);
			singleRaycast(c, i);
		}
	}

	@Override
	public void update(int delta) {
		car.update();
		doRaycasts(car);
		if (mode1.isActive()) {
			mode = 1;
		}
		else if (mode2.isActive()) {
			mode = 2;
		}
		else if (mode3.isActive()) {
			mode = 3;
		}
		if(mode == 1) {
			passCarInputs(up.isActive(), down.isActive(), left.isActive(), right.isActive());
		}
		else {
			float[] nnIns = new float[nnInputs];
			for(int i = 0; i < nnInputs; i++) {
				if(i < numRaycasts) {
					nnIns[i] = raycastDistances[i];
				}
				else if(i == numRaycasts) {
					nnIns[i] = (float) car.getBody().getLinearVelocity().length();
				}
				else if(i == numRaycasts + 1) {
					nnIns[i] = lastUp ? 1 : 0;
				}
				else if(i == numRaycasts + 2) {
					nnIns[i] = lastDown ? 1 : 0;
				}
				else if(i == numRaycasts + 3) {
					nnIns[i] = lastLeft ? 1 : 0;
				}
				else if(i == numRaycasts + 4) {
					nnIns[i] = lastRight ? 1 : 0;
				}
			}
			float[] nnOuts = nn.feedForward(nnIns);
			/*System.out.println("------------------");
			for(int i = 0; i < nnOuts.length; i++) {
				System.out.println(nnOuts[i]);
			}*/
			
			lastUp = nnOuts[0] > 0.5;
			lastDown = nnOuts[1] > 0.5;
			lastLeft = nnOuts[2] > 0.5;
			lastRight = nnOuts[3] > 0.5;
			
			if(mode == 2) {
				passCarInputs(lastUp, lastDown, lastLeft, lastRight);
			}
			else if(mode == 3) {
				// 1. Split in two timelines: one with NN action, one with random action
				// 2. Compare Timelines after time interval
				trainingtimer += delta;
				if(timeline == 0) {
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if(trainingtimer >= timeBetweenSplits) {
						splitPosition.set(car.getTranslation());
						splitRotation.set(car.getRotation());
						splitVelocity.set(car.getBody().getLinearVelocity());
						splitAngularVelocity.set(car.getBody().getAngularVelocity());
						System.arraycopy(nnIns, 0, inputsOnSplit, 0, nnIns.length);
						timeline++;
						trainingtimer = 0;
					}
				}
				else if(timeline == 1) {
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if(trainingtimer >= splitlength) {
						velocityAfterInterval = (float) car.getBody().getLinearVelocity().length();
						car.getTranslation().set(splitPosition);
						car.getRotation().set(splitRotation);
						car.getBody().getLinearVelocity().set(splitVelocity);
						car.getBody().getAngularVelocity().set(splitAngularVelocity);
						timeline++;
						trainingtimer = 0;
					}
				}
				else if(timeline == 2) {
					if(firstIterationOfAlternativeTimeline) {
						int modifiyInput = (int) (Math.random() * 4);
						System.out.println("Modifying: " + modifiyInput);
						switch(modifiyInput) {
						case 0:
							lastUp = !lastUp;
							nnOuts[0] = lastUp ? 1 : 0;
							break;
						case 1:
							lastDown = !lastDown;
							nnOuts[1] = lastDown ? 1 : 0;
							break;
						case 2:
							lastLeft = !lastLeft;
							nnOuts[2] = lastLeft ? 1 : 0;
							break;
						case 3:
							lastRight = !lastRight;
							nnOuts[3] = lastRight ? 1 : 0;
							break;
						}
						System.arraycopy(nnOuts, 0, expectedOutputs, 0, nnOuts.length);
						firstIterationOfAlternativeTimeline = false;
					}
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if(trainingtimer >= splitlength) {
						System.out.println(velocityAfterInterval + " vs " + car.getBody().getLinearVelocity().length());
						if(velocityAfterInterval > car.getBody().getLinearVelocity().length()) {
							car.getTranslation().set(splitPosition);
							car.getRotation().set(splitRotation);
							car.getBody().getLinearVelocity().set(splitVelocity);
							car.getBody().getAngularVelocity().set(splitAngularVelocity);
						}
						else {
							nn.feedForward(inputsOnSplit);
							nn.backProp(expectedOutputs);
						}
						timeline = 0;
						trainingtimer = 0;
						firstIterationOfAlternativeTimeline = true;
					}
				}
			}
		}

		debugger.update(fps, 0, 0);
		space.update(delta);
		physicsdebug.update();
		profiler.update(delta);
	}
	
	private void passCarInputs(boolean up, boolean down, boolean left, boolean right) {
		if (up) {
			car.accelerate();
		}
		if (down) {
			car.brake();
		}
		if (left) {
			car.steerLeft();
		}
		if (right) {
			car.steerRight();
		}
	}
}
