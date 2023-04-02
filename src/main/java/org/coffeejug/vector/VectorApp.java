package org.coffeejug.vector;

import java.util.Arrays;
import java.util.random.RandomGeneratorFactory;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

// --enable-preview --add-modules jdk.incubator.vector
public class VectorApp {

  static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

  public static void main(String[] args) {

    var randomGenerator = RandomGeneratorFactory.getDefault().create();
    final int[] a = randomGenerator.ints(8).toArray();
    final int[] b = randomGenerator.ints(8).toArray();

    // Objective: ((Va * Vb) + (Va * Vb)) * -1V

    int[] scalarResult = scalarComputationTest(a, b);
    System.out.println(Arrays.toString(scalarResult));
    int[] vectorResult = vectorComputationTest(a, b);
    System.out.println(Arrays.toString(vectorResult));

    System.out.println(SPECIES);
    System.out.println("RESULT ASSERTION: " + Arrays.equals(scalarResult, vectorResult));
  }


  private static int[] scalarComputationTest(int[] a, int[] b) {
    final int[] c = new int[a.length];

    for (int i = 0; i < a.length; i++) {
      c[i] = (a[i] * a[i] + b[i] * b[i]) * -1;
    }

    System.out.println(Arrays.stream(c).filter(it -> it > 0).max().orElseThrow());

    return c;
  }

  private static IntVector vectorizedImpl(IntVector va, IntVector vb) {
    return va.mul(va).add(vb.mul(vb)).neg();
  }

  private static int[] vectorComputationTest(int[] a, int[] b) {
    final int[] c = new int[a.length];

    int i = 0;
    var va = IntVector.fromArray(SPECIES, a, i);
    var vb = IntVector.fromArray(SPECIES, b, i);

    IntVector vc = vectorizedImpl(va, vb);
    vc.intoArray(c, i);

    VectorMask<Integer> compare = vc.compare(VectorOperators.GT, 0);
    System.out.println(compare);
    int maxResult = vc.reduceLanes(VectorOperators.MAX, compare);
    System.out.println(maxResult);
    return c;
  }

  private static int[] vectorComputationTestOpt(int[] a, int[] b) {
    final int[] c = new int[a.length];

    int i = 0;
    int upperBound = SPECIES.loopBound(a.length);
    for (; i < upperBound; i += SPECIES.length()) {
      var va = IntVector.fromArray(SPECIES, a, i);
      var vb = IntVector.fromArray(SPECIES, b, i);
      IntVector vc = vectorizedImpl(va, vb);
      vc.intoArray(c, i);
    }

    for (; i < a.length; i++) {
      c[i] = (a[i] * a[i] + b[i] * b[i]) * -1;
    }

    return c;
  }
}
