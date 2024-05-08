import java.util.Random;

public class Matrix {
    double[][] matrix;


    public Matrix(double[][] matrix) {
        this.matrix = matrix;
    }

    public Matrix(int numRows, int numCols) {
        this.matrix = new double[numRows][numCols];
    }

    public static Matrix generateRandom(int numRows, int numCols) {
        double[][] matrix = new double[numCols][numRows];
        Random rand = new Random();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                matrix[i][j] = (double) rand.nextInt(10) + 1;
            }
        }
        return new Matrix(matrix);
    }

    void print() {
        for (double[] doubles : matrix) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(doubles[j] + " ");
            }
            System.out.println();
        }
    }

    void add(int i, int j, double value) {
        matrix[i][j] += value;
    }


    public Matrix multiply(Matrix B) {
        double[][] result = new double[matrix.length][B.matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < B.matrix[0].length; j++) {
                for (int k = 0; k < B.matrix.length; k++) {
                    result[i][j] += matrix[i][k] * B.matrix[k][j];
                }
            }
        }
        return new Matrix(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix other) {
            if (matrix.length != other.matrix.length || matrix[0].length != other.matrix[0].length) {
                return false;
            }
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (matrix[i][j] != other.matrix[i][j]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}