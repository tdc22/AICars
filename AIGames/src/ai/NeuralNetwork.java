package ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

public class NeuralNetwork {
	Layer[] layers;
	Random random;
	float learningRate = 0.02f; // 0.01

	public NeuralNetwork(int[] layer) {
		random = new Random();
		layers = new Layer[layer.length - 1];
		for (int i = 0; i < layers.length; i++) {
			layers[i] = new Layer(layer[i], layer[i + 1]);
		}
	}

	public float[] feedForward(float[] inputs) {
		layers[0].feedForward(inputs);
		for (int i = 1; i < layers.length; i++) {
			layers[i].feedForward(layers[i - 1].outputs);
		}
		return layers[layers.length - 1].outputs;
	}

	public void backProp(float[] expected) {
		for (int i = layers.length - 1; i >= 0; i--) {
			if (i == layers.length - 1) {
				layers[i].backPropOutput(expected);
			} else {
				layers[i].backPropHidden(layers[i + 1].gamma, layers[i + 1].weights);
			}
		}
		for (int i = 0; i < layers.length; i++) {
			layers[i].updateWeights();
		}
	}

	StringBuilder sb = new StringBuilder();

	public String toString(long savingCounter) {
		sb.setLength(0);
		sb.append('#');
		sb.append(savingCounter);
		sb.append('\n');
		for (int i = 0; i < layers.length; i++) {
			Layer l = layers[i];
			for (int j = 0; j < l.numberOfOutputs; j++) {
				for (int k = 0; k < l.numberOfInputs; k++) {
					sb.append(l.weights[j][k]).append(',');
				}
				sb.append('\n');
			}
			sb.append("/\n");
		}
		return sb.toString();
	}

	public int parse(BufferedReader r) {
		LinkedList<Layer> ls = new LinkedList<Layer>();
		boolean firstLineOfLayer = true;
		int numIns = 0, numOuts = 0;
		int savingIntervalCount = 0;
		LinkedList<Float[]> ws = new LinkedList<Float[]>();
		String s;
		try {
			savingIntervalCount = Integer.parseInt(r.readLine().replace("#", ""));
			while ((s = r.readLine()) != null) {
				if (s.equals("/")) {
					numOuts = ws.size();
					if (numOuts > 0) {
						Layer l = new Layer(numIns, numOuts);
						for (int x = 0; x < numOuts; x++) {
							Float[] we = ws.pop();
							for (int y = 0; y < numIns; y++) {
								l.weights[x][y] = we[y];
							}
						}
						ls.add(l);
						ws.clear();
					}
					firstLineOfLayer = true;
				} else {
					String[] lineparts = s.split(",");
					if (firstLineOfLayer) {
						numIns = lineparts.length;
					}
					Float[] w = new Float[lineparts.length];
					for (int i = 0; i < lineparts.length; i++) {
						w[i] = Float.parseFloat(lineparts[i]);
					}
					ws.add(w);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		int layercount = ls.size();
		this.layers = new Layer[layercount];
		for (int i = 0; i < layercount; i++) {
			layers[i] = ls.pop();
		}
		return savingIntervalCount;
	}

	public class Layer {
		int numberOfInputs, numberOfOutputs;

		float[] inputs, outputs, gamma, error;
		float[][] weights, weightsDelta;

		public Layer(int numInputs, int numOutputs) {
			this.numberOfInputs = numInputs;
			this.numberOfOutputs = numOutputs;

			inputs = new float[numInputs];
			outputs = new float[numOutputs];
			weights = new float[numOutputs][numInputs];
			weightsDelta = new float[numOutputs][numInputs];
			gamma = new float[numberOfOutputs];
			error = new float[numberOfOutputs];

			initWeights();
		}

		private void initWeights() {
			for (int i = 0; i < numberOfOutputs; i++) {
				for (int j = 0; j < numberOfInputs; j++) {
					weights[i][j] = random.nextFloat() - 0.5f;
				}
			}
		}

		public float[] feedForward(float[] input) {
			inputs = input;
			for (int i = 0; i < numberOfOutputs; i++) {
				outputs[i] = 0;
				for (int j = 0; j < numberOfInputs; j++) {
					outputs[i] += inputs[j] * weights[i][j];
				}
				outputs[i] = (float) Math.tanh(outputs[i]);
			}
			return outputs;
		}

		public float tanHDer(float value) {
			return 1 - (value * value);
		}

		public void backPropOutput(float[] expected) {
			for (int i = 0; i < numberOfOutputs; i++) {
				error[i] = outputs[i] - expected[i];
			}

			for (int i = 0; i < numberOfOutputs; i++) {
				gamma[i] = error[i] * tanHDer(outputs[i]);
			}

			for (int i = 0; i < numberOfOutputs; i++) {
				for (int j = 0; j < numberOfInputs; j++) {
					weightsDelta[i][j] = gamma[i] * inputs[j];
				}
			}
		}

		public void backPropHidden(float[] gammaForward, float[][] weightsForward) {
			for (int i = 0; i < numberOfOutputs; i++) {
				gamma[i] = 0;
				for (int j = 0; j < gammaForward.length; j++) {
					gamma[i] += gammaForward[j] * weightsForward[j][i];
				}
				gamma[i] *= tanHDer(outputs[i]);
			}

			for (int i = 0; i < numberOfOutputs; i++) {
				for (int j = 0; j < numberOfInputs; j++) {
					weightsDelta[i][j] = gamma[i] * inputs[j];
				}
			}
		}

		public void updateWeights() {
			for (int i = 0; i < numberOfOutputs; i++) {
				for (int j = 0; j < numberOfInputs; j++) {
					weights[i][j] -= weightsDelta[i][j] * learningRate;
				}
			}
		}
	}
}