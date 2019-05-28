package ai;

import java.util.Arrays;
import java.util.Random;

public class NeuralNetwork {
	int[] layer;
	Layer[] layers;
	Random random;
	float learningRate = 0.01f;
	
	public NeuralNetwork(int[] layer) {
		this.layer = Arrays.copyOf(layer, layer.length);
		random = new Random();
		layers = new Layer[layer.length - 1];
		for(int i = 0; i < layers.length; i++) {
			layers[i] = new Layer(layer[i], layer[i+1]);
		}
	}
	
	public float[] feedForward(float[] inputs) {
		layers[0].feedForward(inputs);
		for(int i = 1; i < layers.length; i++) {
			layers[i].feedForward(layers[i - 1].outputs);
		}
		return layers[layers.length - 1].outputs;
	}
	
	public void backProp(float[] expected) {
		for(int i = layers.length-1; i >= 0; i--) {
			if(i == layers.length - 1) {
				layers[i].backPropOutput(expected);
			}
			else {
				layers[i].backPropHidden(layers[i+1].gamma, layers[i+1].weights);
			}
		}
		for(int i = 0; i < layers.length; i++) {
			layers[i].updateWeights();
		}
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
			for(int i = 0; i < numberOfOutputs; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					weights[i][j] = random.nextFloat() - 0.5f;
				}
			}
		}
		
		public float[] feedForward(float[] input) {
			inputs = input;
			for(int i = 0; i < numberOfOutputs; i++) {
				outputs[i] = 0;
				for(int j = 0; j < numberOfInputs; j++) {
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
			for(int i = 0; i < numberOfOutputs; i++) {
				error[i] = outputs[i] - expected[i];
			}
			
			for(int i = 0; i < numberOfOutputs; i++) {
				gamma[i] = error[i] * tanHDer(outputs[i]);
			}
			
			for(int i = 0; i < numberOfOutputs; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					weightsDelta[i][j] = gamma[i] * inputs[j];
				}
			}
		}
		
		public void backPropHidden(float[] gammaForward, float[][] weightsForward) {
			for(int i = 0; i < numberOfOutputs; i++) {
				gamma[i] = 0;
				for(int j = 0; j < gammaForward.length; j++) {
					gamma[i] += gammaForward[j] * weightsForward[j][i];
				}
				gamma[i] *= tanHDer(outputs[i]);
			}
			
			for(int i = 0; i < numberOfOutputs; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					weightsDelta[i][j] = gamma[i] * inputs[j];
				}
			}
		}
		
		public void updateWeights() {
			for(int i = 0; i < numberOfOutputs; i++) {
				for(int j = 0; j < numberOfInputs; j++) {
					weights[i][j] -= weightsDelta[i][j] * learningRate;
				}
			}
		}
	}
}