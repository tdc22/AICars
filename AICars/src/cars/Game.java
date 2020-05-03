package cars;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ai.NeuralNetwork;
import broadphase.DynamicAABBTree2;
import display.DisplayMode;
import display.GLDisplay;
import display.PixelFormat;
import display.VideoSettings;
import game.StandardGame;
import input.Input;
import input.InputEvent;
import input.KeyInput;
import integration.VerletIntegration;
import loader.ShaderLoader;
import loader.TextureLoader;
import manifold.MultiPointManifoldManager2;
import manifold.RaycastResult;
import math.VecMath;
import narrowphase.EPA2;
import narrowphase.GJK2;
import narrowphase.SupportRaycast2;
import objects.Ray2;
import objects.RigidBody;
import physics.PhysicsSpace2;
import positionalcorrection.ProjectionCorrection;
import quaternion.Complexf;
import resolution.ImpulseResolution;
import shader.Shader;
import shape2d.Circle;
import shape2d.Quad;
import sound.NullSoundEnvironment;
import texture.Texture;
import vector.Vector1f;
import vector.Vector2f;
import vector.Vector4f;

public class Game extends StandardGame {
	PhysicsSpace2 space;
	int tempdelta = 0;
	Shader defaultshader;

	Car car;
	int mode = 1;
	InputEvent up, down, left, right, mode1, mode2, mode3, toggleRendering;
	Quad arrowUp, arrowDown, arrowLeft, arrowRight;
	final int numRaycasts = 10;
	Circle[] raycastTrackers;
	float[] raycastDistances;
	boolean isRendering = true;

	final String nnFilename = "NN13_CircleTrack";
	final String nnFilepath = "res/networks/" + nnFilename;
	NeuralNetwork nn;
	Random random;
	int nnInputs = numRaycasts + 8;
	float undefinedRayDistance = 0;
	boolean lastLeft = false, lastRight = false, lastUp = false, lastDown = false;
	float lastVelocity = 0;

	int trainingtimer = 0;
	int controltimer = 0;
	final int timeBetweenSplits = 0;
	final int minSplitLength = 3000;
	final int maxSplitLength = 8000;
	final int maxControllLength = 3000;
	final int splitLengthInterval = maxSplitLength - minSplitLength;
	final int maxIterationsInTimeline = maxSplitLength / 16;
	int splitlength;
	int controllength;
	int timeline = 0;
	int trainingsIterationsSinceSave = 0;
	final int savingInterval = 1000;
	long savingIntervalCount = 0;
	ArrayDeque<boolean[]> firstTimelineOutputs;
	float velocityAfterInterval;
	float summedVelocityInTimeline1, summedVelocityInTimeline2;
	float progressAtStart, progressAfterTimeline1, progressAfterTimeline2;
	boolean nextLapTimeline1, nextLapTimeline2;
	Vector2f splitPosition = new Vector2f();
	Complexf splitRotation = new Complexf();
	Vector2f splitVelocity = new Vector2f();
	Vector2f splitForceAccumulator = new Vector2f();
	Vector1f splitAngularVelocity = new Vector1f();
	float[] inputsOnSplit;
	float[] expectedOutputs;
	boolean firstIterationOfTimeline = true;
	ArrayDeque<boolean[]> outputstorage;
	Vector2f[] trackpoints;
	Vector2f[] tracktangents;
	float[] tracklengths;
	Vector2f lastTP, nextTP, lastTangent, nextTangent, trackDir;
	int currentSegment = 0, segmentCount;
	boolean learndSomethingInSavingInterval = false;

	// normalization
	final float maxVelocity = 601;
	final float halfMaxVelocity = maxVelocity / 2f;
	final float maxSightRange = 2000;
	final float halfMaxSightRange = maxSightRange / 2f;

	@Override
	public void init() {
		initDisplay(new GLDisplay(), new DisplayMode(1280, 720, "AICars " + nnFilename, false), new PixelFormat(),
				new VideoSettings(1920, 1080), new NullSoundEnvironment());

		int defaultShaderID = ShaderLoader.loadShaderFromFile("res/shaders/defaultshader.vert",
				"res/shaders/defaultshader.frag");
		Shader defaultshader3 = new Shader(defaultShaderID);
		addShader(defaultshader3);
		defaultshader = new Shader(defaultShaderID);
		addShader2d(defaultshader);
		Shader defaultshaderInterface = new Shader(defaultShaderID);
		addShaderInterface(defaultshaderInterface);
		Shader raycasttrackershader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/colorshader.vert", "res/shaders/colorshader.frag"));
		raycasttrackershader.addArgument("u_color", new Vector4f(1, 0, 0, 1));
		addShader2d(raycasttrackershader);

		Texture texture = new Texture(TextureLoader.loadTexture("res/textures/arrow.png"));
		Shader textureshader = new Shader(
				ShaderLoader.loadShaderFromFile("res/shaders/arrowshader.vert", "res/shaders/arrowshader.frag"));
		textureshader.addArgument("u_texture", texture);
		addShader2d(textureshader);

		space = new PhysicsSpace2(new VerletIntegration(), new DynamicAABBTree2(), new GJK2(new EPA2()),
				new SupportRaycast2(), new ImpulseResolution(), new ProjectionCorrection(1),
				new MultiPointManifoldManager2());

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
		toggleRendering = new InputEvent("ToggleRendering",
				new Input(Input.KEYBOARD_EVENT, "Tab", KeyInput.KEY_PRESSED));

		inputs.addEvent(up);
		inputs.addEvent(down);
		inputs.addEvent(left);
		inputs.addEvent(right);
		inputs.addEvent(mode1);
		inputs.addEvent(mode2);
		inputs.addEvent(mode3);
		inputs.addEvent(toggleRendering);

		initTrackData();
		car = new Car(trackpoints[0].x, trackpoints[0].y, numRaycasts);
		for(int i = 0; i < numRaycasts; i++) {
			space.addRaycastFilter(car.getBody(), car.rays[i]);
		}
		space.addRigidBody(car, car.getBody());
		defaultshader.addObject(car);

		raycastTrackers = new Circle[numRaycasts];
		raycastDistances = new float[numRaycasts];
		for (int i = 0; i < numRaycasts; i++) {
			Circle c = new Circle(0, 0, 5, 18);
			raycasttrackershader.addObject(c);
			raycastTrackers[i] = c;
			raycastDistances[i] = undefinedRayDistance;
		}
		rayrotation.rotate(360 / (float) numRaycasts);

		int nnInputcount = nnInputs;
		int nnOutputcount = 4;
		nn = new NeuralNetwork(new int[] { nnInputcount, 30, 30, 30, 10, nnOutputcount });

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(nnFilepath));
			savingIntervalCount = nn.parse(br);
			br.close();
		} catch (IOException e) {
			System.out.println("Couldn't find input file! Starting with random NN.");
		}

		inputsOnSplit = new float[nnInputcount];
		expectedOutputs = new float[nnOutputcount];
		firstTimelineOutputs = new ArrayDeque<boolean[]>();
		random = new Random();
		setRendered(false, true, false);

		arrowUp = new Quad(75, 25, 25, 25);
		arrowDown = new Quad(75, 75, 25, 25);
		arrowLeft = new Quad(25, 75, 25, 25);
		arrowRight = new Quad(125, 75, 25, 25);
		arrowDown.rotate(180);
		arrowLeft.rotate(-90);
		arrowRight.rotate(90);
		arrowUp.setRenderHints(true, true, false);
		arrowDown.setRenderHints(true, true, false);
		arrowLeft.setRenderHints(true, true, false);
		arrowRight.setRenderHints(true, true, false);
		textureshader.addObject(arrowUp);
		textureshader.addObject(arrowDown);
		textureshader.addObject(arrowLeft);
		textureshader.addObject(arrowRight);

		outputstorage = new ArrayDeque<boolean[]>();
		for (int i = 0; i < maxIterationsInTimeline; i++) {
			outputstorage.add(new boolean[4]);
		}
	}

	private void addWall(Vector2f a, Vector2f b, Vector2f c, Vector2f d) {
		Wall w = new Wall(a, b, c, d);
		space.addRigidBody(w, w.getBody());
		defaultshader.addObject(w);
	}

	private void initTrackData() {
		/*initTrack(new Vector2f[] { new Vector2f(600, 220), new Vector2f(1000, 220), new Vector2f(1600, 300),
				new Vector2f(1700, 600), new Vector2f(1600, 900), new Vector2f(1300, 900), new Vector2f(1300, 700),
				new Vector2f(1300, 600), new Vector2f(1200, 500), new Vector2f(1100, 500), new Vector2f(500, 900),
				new Vector2f(200, 700), new Vector2f(400, 400) },
				new float[] { 120, 120, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 });*/
		initTrack(new Vector2f[] { new Vector2f(950, 200), new Vector2f(1500, 500), new Vector2f(950, 900),
				new Vector2f(400, 500) },
				new float[] { 180, 250, 180, 250 });
	}

	private void initTrack(Vector2f[] trackpoints, float[] trackwidths) {
		this.trackpoints = trackpoints;
		for (Vector2f v : trackpoints)
			defaultshader.addObject(new Circle(v.x, v.y, 10, 36));

		segmentCount = trackpoints.length;
		tracktangents = new Vector2f[segmentCount];
		tracklengths = new float[segmentCount];
		lastTP = new Vector2f(trackpoints[0]);
		nextTP = new Vector2f(trackpoints[1]);
		trackDir = VecMath.subtraction(nextTP, lastTP);
		trackDir.normalize();

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

			Vector2f normalA = VecMath.subtraction(pb, plast);
			tracktangents[i] = new Vector2f(normalA);
			normalA.set(normalA.y, -normalA.x);
			normalA.normalize();
			Vector2f normalB = VecMath.subtraction(pnext, pa);
			normalB.set(normalB.y, -normalB.x);
			normalB.normalize();
			
			tracklengths[i] = (float) VecMath.subtraction(pb, pa).length();

			Vector2f a1 = VecMath.scale(normalA, wa);
			Vector2f b1 = VecMath.scale(normalA, wa + 20f);
			Vector2f c1 = VecMath.scale(normalB, wb + 20f);
			Vector2f d1 = VecMath.scale(normalB, wb);

			a1.translate(pa);
			b1.translate(pa);
			c1.translate(pb);
			d1.translate(pb);

			addWall(a1, d1, c1, b1);

			Vector2f a2 = VecMath.scale(normalA, -wa);
			Vector2f b2 = VecMath.scale(normalA, -wa + 20f);
			Vector2f c2 = VecMath.scale(normalB, -wb + 20f);
			Vector2f d2 = VecMath.scale(normalB, -wb);

			a2.translate(pa);
			b2.translate(pa);
			c2.translate(pb);
			d2.translate(pb);

			addWall(a2, d2, c2, b2);

			plast = pa;
		}
		currentSegmentFactor = tracklengths[0] * segmentCount;
		lastTangent = new Vector2f(tracktangents[0]);
		nextTangent = new Vector2f(tracktangents[1]);
	}

	@Override
	public void render() {

	}

	@Override
	public void render2d() {
		render2dLayer();
	}

	@Override
	public void renderInterface() {
		renderInterfaceLayer();
	}

	private Complexf rayrotation = new Complexf();
	private Vector2f lastdirection = new Vector2f();
	private Vector2f raystart = new Vector2f();
	
	boolean errorstop = false;

	private void singleRaycast(Car c, int i) {
		Ray2 ray = c.rays[i];
		ray.getDirection().set(lastdirection);
		raystart.set(c.raystartpoints[i]);
		raystart.transform(c.getRotation());
		raystart.translate(c.getTranslation());
		ray.getPosition().set(raystart);
		RaycastResult<Vector2f> rr = space.raycast(ray);
		if (rr != null) {
			raycastTrackers[i].translateTo(rr.getHitPosition());
			raycastDistances[i] = rr.getHitDistance();
		} else {
			//raycastTrackers[i].translateTo(-100, -100);
			raycastDistances[i] = undefinedRayDistance;
			errorstop = true;
			mode = 1;
			Set<RigidBody<Vector2f, ?, Complexf, ?>> overlaps = space.raycastAllBroadphase(ray);
			System.out.println("RC Overlaps: " + overlaps.size());
			Iterator<RigidBody<Vector2f, ?, Complexf, ?>> it = overlaps.iterator();
			for(int j = 0; j < overlaps.size(); j++) {
				RigidBody<Vector2f, ?, Complexf, ?> rb = it.next();
				System.out.println(j + "; " + rb.getTranslation());
				if(Math.random() > 0.5) {
					raycastTrackers[i].translateTo(rb.getTranslation());
				}
			}
			if(Math.random() > 0.7) {
				Vector2f d = new Vector2f(ray.getDirection());
				d.scale(50);
				d.translate(c.getTranslation());
				raycastTrackers[i].translateTo(d);
			}
			if(Math.random() > 0.85) {
				raycastTrackers[i].translateTo(ray.getPosition());
			}
			System.out.println("Raycast Error! " + this.trainingsIterationsSinceSave + "; " + i + "; " + c.getTranslation() + "; " + c.getRotation() + "; " + ray.getPosition() + "; " + ray.getDirection());
		}
	}

	private void doRaycasts(Car c) {
		lastdirection.set(c.direction);
		singleRaycast(c, 0);
		for (int i = 1; i < numRaycasts; i++) {
			lastdirection.transform(rayrotation);
			singleRaycast(c, i);
		}
	}
	
	boolean forward = true;
	Vector2f cA = new Vector2f(), cB = new Vector2f();
	float progress;
	float currentSegmentFactor;
	boolean nextLap;
	private void updateSegment(Car c) {
		Vector2f pos = c.getTranslation();
		cA.set(pos.x - lastTP.x, pos.y - lastTP.y);
		cB.set(pos.x - nextTP.x, pos.y - nextTP.y);
		if(VecMath.dotproduct(cB, nextTangent) > 0) {
			currentSegment = (currentSegment + 1) % segmentCount;
			if(currentSegment == 0) {
				nextLap = forward;
				forward = true;
			}
			lastTP.set(nextTP);
			int nextindex = (currentSegment + 1) % segmentCount;
			nextTP.set(trackpoints[nextindex]);
			if(forward) {
				trackDir.set(nextTP.x - lastTP.x, nextTP.y - lastTP.y);
			}
			else {
				trackDir.set(lastTP.x - nextTP.x, lastTP.y - nextTP.y);
			}
			trackDir.normalize();
			lastTangent.set(nextTangent);
			nextTangent.set(tracktangents[nextindex]);
			currentSegmentFactor = this.tracklengths[currentSegment] * segmentCount;
		}
		else if(VecMath.dotproduct(cA, lastTangent) < 0) {
			if(currentSegment == 0) {
				forward = false;
			}
			currentSegment = currentSegment > 0 ? currentSegment - 1 : segmentCount - 1;
			nextTP.set(lastTP);
			int lastindex = currentSegment;
			lastTP.set(trackpoints[lastindex]);
			if(forward) {
				trackDir.set(nextTP.x - lastTP.x, nextTP.y - lastTP.y);
			}
			else {
				trackDir.set(lastTP.x - nextTP.x, lastTP.y - nextTP.y);
			}
			trackDir.normalize();
			nextTangent.set(lastTangent);
			lastTangent.set(tracktangents[lastindex]);
			currentSegmentFactor = this.tracklengths[currentSegment] * segmentCount;
		}
		if(forward) {
			progress = currentSegment / (float) segmentCount + VecMath.dotproduct(cA, trackDir) / currentSegmentFactor;
		}
		else {
			progress = -((segmentCount - currentSegment - 1) / (float) segmentCount + VecMath.dotproduct(cB, trackDir) / currentSegmentFactor);
		}
	}

	float[] nnIns = new float[nnInputs];

	@Override
	public void update(int delta) {
		delta = 16; // Fixing timestep to avoid diverging timeline lengths
		car.update();
		doRaycasts(car);
		updateSegment(car);
		if (mode1.isActive()) {
			mode = 1;
		} else if (mode2.isActive()) {
			mode = 2;
		} else if (mode3.isActive()) {
			mode = 3;
			errorstop = false;
		}
		if (toggleRendering.isActive()) {
			isRendering = !isRendering;
			if (isRendering) {
				inputs.addEvent(up);
				inputs.addEvent(down);
				inputs.addEvent(left);
				inputs.addEvent(right);
				inputs.addEvent(mode1);
				inputs.addEvent(mode2);
				inputs.addEvent(mode3);
			} else {
				inputs.removeEvent(up);
				inputs.removeEvent(down);
				inputs.removeEvent(left);
				inputs.removeEvent(right);
				inputs.removeEvent(mode1);
				inputs.removeEvent(mode2);
				inputs.removeEvent(mode3);
			}
			setRendered(false, isRendering, false);
		}
		if (mode == 1) {
			passCarInputs(up.isActive(), down.isActive(), left.isActive(), right.isActive());
		} else {
			float[] nnOuts = null;
			if (mode != 3 || timeline < 2 || firstIterationOfTimeline) {
				for (int i = 0; i < numRaycasts; i++) {
					nnIns[i] = Math.min(raycastDistances[i], maxSightRange) / halfMaxSightRange - 1;
				}
				nnIns[numRaycasts] = (float) car.getBody().getLinearVelocity().length() / halfMaxVelocity - 1;
				nnIns[numRaycasts + 1] = VecMath.dotproduct(car.getBody().getLinearVelocity(), car.direction) >= 0 ? 1 : -1;
				nnIns[numRaycasts + 2] = lastUp ? 1 : -1;
				nnIns[numRaycasts + 3] = lastDown ? 1 : -1;
				nnIns[numRaycasts + 4] = lastLeft ? 1 : -1;
				nnIns[numRaycasts + 5] = lastRight ? 1 : -1;
				float forwardfactor = forward ? 1 : -1;
				nnIns[numRaycasts + 6] = (forwardfactor * VecMath.dotproduct(car.direction, trackDir) > 0) ? 1 : -1;
				nnIns[numRaycasts + 7] = (forwardfactor * VecMath.dotproduct(car.getBody().getLinearVelocity(), trackDir) > 0) ? 1 : -1;
				nnOuts = nn.feedForward(nnIns);
				lastUp = nnOuts[0] > 0;
				lastDown = nnOuts[1] > 0;
				lastLeft = nnOuts[2] > 0;
				lastRight = nnOuts[3] > 0;
			}

			if (mode == 2) {
				passCarInputs(lastUp, lastDown, lastLeft, lastRight);
			} else if (mode == 3) {
				// 1. Split in two timelines: one with NN action, one with random action
				// 2. Compare Timelines after time interval
				trainingtimer += delta;
				if (timeline == 0) {
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if (trainingtimer >= timeBetweenSplits) {
						splitPosition.set(car.getTranslation());
						splitRotation.set(car.getRotation());
						splitVelocity.set(car.getBody().getLinearVelocity());
						splitAngularVelocity.set(car.getBody().getAngularVelocity());
						splitForceAccumulator.set(car.getBody().getForceAccumulator());
						timeline++;
						trainingtimer = 0;
						for (int i = 0; i < firstTimelineOutputs.size(); i++) {
							outputstorage.add(firstTimelineOutputs.pop());
						}
						summedVelocityInTimeline1 = 0;
						summedVelocityInTimeline2 = 0;
						nextLap = false;
						progressAtStart = progress;
						splitlength = minSplitLength + (int) (random.nextFloat() * splitLengthInterval);
						controllength = (int) (random.nextFloat() * maxControllLength);
					}
				} else if (timeline == 1) {
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if (!firstIterationOfTimeline) {
						boolean[] outputarray = outputstorage.pop();
						outputarray[0] = lastUp;
						outputarray[1] = lastDown;
						outputarray[2] = lastLeft;
						outputarray[3] = lastRight;
						firstTimelineOutputs.add(outputarray);
						summedVelocityInTimeline1 += car.getBody().getLinearVelocity().length();
					} else {
						System.arraycopy(nnIns, 0, inputsOnSplit, 0, nnIns.length);
						firstIterationOfTimeline = false;
					}
					if (trainingtimer >= splitlength) {
						progressAfterTimeline1 = progress;
						nextLapTimeline1 = nextLap;
						nextLap = false;
						car.getTranslation().set(splitPosition);
						car.getRotation().set(splitRotation);
						car.getBody().getLinearVelocity().set(splitVelocity);
						car.getBody().getAngularVelocity().set(splitAngularVelocity);
						car.getBody().getForceAccumulator().set(splitForceAccumulator);
						timeline++;
						trainingtimer = 0;
						firstIterationOfTimeline = true;
					}
				} else if (timeline == 2) {
					if (firstIterationOfTimeline) {
						if (random.nextFloat() < 0.9) {
							int modifiyInput = (int) (random.nextFloat() * 4);
							switch (modifiyInput) {
							case 0:
								lastUp = !lastUp;
								nnOuts[0] = lastUp ? 1 : -1;
								break;
							case 1:
								lastDown = !lastDown;
								nnOuts[1] = lastDown ? 1 : -1;
								break;
							case 2:
								lastLeft = !lastLeft;
								nnOuts[2] = lastLeft ? 1 : -1;
								break;
							case 3:
								lastRight = !lastRight;
								nnOuts[3] = lastRight ? 1 : -1;
								break;
							}
						} else {
							lastUp = random.nextFloat() < 0.5;
							lastDown = random.nextFloat() < 0.5;
							lastLeft = random.nextFloat() < 0.5;
							lastRight = random.nextFloat() < 0.5;
							nnOuts[0] = lastUp ? 1 : -1;
							nnOuts[1] = lastDown ? 1 : -1;
							nnOuts[2] = lastLeft ? 1 : -1;
							nnOuts[3] = lastRight ? 1 : -1;
						}
						System.arraycopy(nnOuts, 0, expectedOutputs, 0, nnOuts.length);
						firstIterationOfTimeline = false;
					} else {
						summedVelocityInTimeline2 += car.getBody().getLinearVelocity().length();
						boolean[] outputs = firstTimelineOutputs.pop();
						if (controltimer < controllength) {
							controltimer += delta;
						} else {
							lastUp = outputs[0];
							lastDown = outputs[1];
							lastLeft = outputs[2];
							lastRight = outputs[3];
						}
						outputstorage.add(outputs);
					}
					passCarInputs(lastUp, lastDown, lastLeft, lastRight);
					if (trainingtimer >= splitlength) {
						progressAfterTimeline2 = progress;
						nextLapTimeline2 = nextLap;
						float progressTimeline1 = progressAfterTimeline1 - progressAtStart;
						float progressTimeline2 = progressAfterTimeline2 - progressAtStart;
						if (progressTimeline1 < progressTimeline2 && progressTimeline2 > 0 || (!nextLapTimeline1 && nextLapTimeline2)) {
							System.out.println(progressTimeline1 + "; " + progressTimeline2 + "; " + nextLapTimeline1 + "; " + nextLapTimeline2);
							nn.feedForward(inputsOnSplit);
							for (int i = 0; i < expectedOutputs.length; i++)
								System.out.print(expectedOutputs[i] + "; ");
							System.out.println();
							nn.backProp(expectedOutputs);
							learndSomethingInSavingInterval = true;
						}
						car.getTranslation().set(splitPosition);
						car.getRotation().set(splitRotation);
						car.getBody().getLinearVelocity().set(splitVelocity);
						car.getBody().getAngularVelocity().set(splitAngularVelocity);
						car.getBody().getForceAccumulator().set(splitForceAccumulator);
						timeline = 0;
						controltimer = 0;
						trainingtimer = 0;
						firstIterationOfTimeline = true;

						trainingsIterationsSinceSave++;
						if (trainingsIterationsSinceSave >= savingInterval) {
							savingIntervalCount++;
							try {
								BufferedWriter writer = new BufferedWriter(new FileWriter(nnFilepath));
								writer.write(nn.toString(savingIntervalCount));
								writer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println("Saved! " + savingIntervalCount);
							trainingsIterationsSinceSave = 0;
							if (!learndSomethingInSavingInterval) {
								System.out.println("Learned nothing, change position!");
								car.getTranslation().set(trackpoints[random.nextInt(trackpoints.length)]);
							}
							learndSomethingInSavingInterval = false;
						}
					}
				}
			}
		}
		if(!errorstop) {
			space.update(delta);
		}
	}

	private void passCarInputs(boolean up, boolean down, boolean left, boolean right) {
		arrowUp.setRendered(up);
		arrowDown.setRendered(down);
		arrowLeft.setRendered(left);
		arrowRight.setRendered(right);
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