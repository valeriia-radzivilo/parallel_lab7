import mpi.MPI;
import mpi.MPIException;

public class Main {

    static final boolean PRINT_MATRICES = false;

    public static void main(String[] args) throws MPIException {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int N = 2000;

        if (N % size != 0) {
            throw new IllegalArgumentException("Matrix size should be divisible by number of processors");
        }


        Matrix A = Matrix.generateRandom(N, N);
        Matrix B = Matrix.generateRandom(N, N);
        Matrix C = new Matrix(N, N);

        if (rank == 0 && PRINT_MATRICES) {
            System.out.println("Matrix A:");
            A.print();
            System.out.println("Matrix B:");
            B.print();
        }


        MPI.COMM_WORLD.Bcast(A.matrix, 0, N, MPI.OBJECT, 0);
        MPI.COMM_WORLD.Bcast(B.matrix, 0, N, MPI.OBJECT, 0);


        int from = rank * N / size;
        int to = (rank + 1) * N / size;


        for (int i = from; i < to; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    C.add(i, j, A.matrix[i][k] * B.matrix[k][j]);
                }
            }
        }


        MPI.COMM_WORLD.Gather(C.matrix, from, to - from, MPI.OBJECT, C.matrix, 0, to - from, MPI.OBJECT, 0);


        if (rank == 0) {
            if (PRINT_MATRICES) {
                System.out.println("Result:");
                C.print();
            }
            // Check result
            Matrix expected = A.multiply(B);
            if (C.equals(expected)) {
                System.out.println("Results are correct!");
            } else {
                System.out.println("Error in the result");
            }
        }
        MPI.Finalize();
    }
}