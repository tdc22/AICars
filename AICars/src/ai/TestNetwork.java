package ai;

public class TestNetwork {
	public void testXOR() {
		// 0 0 0 => 0
		// 0 0 1 => 1
		// 0 1 0 => 1
		// 0 1 1 => 0
		// 1 0 0 => 1
		// 1 0 1 => 0
		// 1 1 0 => 0
		// 1 1 1 => 1
		NeuralNetwork nn = new NeuralNetwork(new int[] { 3, 25, 25, 1 });
		for (int i = 0; i < 5000; i++) {
			nn.feedForward(new float[] { 0, 0, 0 });
			nn.backProp(new float[] { 0 });

			nn.feedForward(new float[] { 0, 0, 1 });
			nn.backProp(new float[] { 1 });

			nn.feedForward(new float[] { 0, 1, 0 });
			nn.backProp(new float[] { 1 });

			nn.feedForward(new float[] { 0, 1, 1 });
			nn.backProp(new float[] { 0 });

			nn.feedForward(new float[] { 1, 0, 0 });
			nn.backProp(new float[] { 1 });

			nn.feedForward(new float[] { 1, 0, 1 });
			nn.backProp(new float[] { 0 });

			nn.feedForward(new float[] { 1, 1, 0 });
			nn.backProp(new float[] { 0 });

			nn.feedForward(new float[] { 1, 1, 1 });
			nn.backProp(new float[] { 1 });
		}
		System.out.println(nn.feedForward(new float[] { 0, 0, 0 })[0]);
		System.out.println(nn.feedForward(new float[] { 0, 0, 1 })[0]);
		System.out.println(nn.feedForward(new float[] { 0, 1, 0 })[0]);
		System.out.println(nn.feedForward(new float[] { 0, 1, 1 })[0]);
		System.out.println(nn.feedForward(new float[] { 1, 0, 0 })[0]);
		System.out.println(nn.feedForward(new float[] { 1, 0, 1 })[0]);
		System.out.println(nn.feedForward(new float[] { 1, 1, 0 })[0]);
		System.out.println(nn.feedForward(new float[] { 1, 1, 1 })[0]);
	}

	public static void main(String[] args) {
		TestNetwork tn = new TestNetwork();
		tn.testXOR();
	}
}