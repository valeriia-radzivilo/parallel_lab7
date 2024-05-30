import mpi.MPI;
import mpi.MPIException;

import java.util.List;

public class Task1Collective {

    static final boolean PRINT_MATRICES = true;

    static final List<Integer> sizes = List.of(
            10, 1000
//            1000, 2000,
//            3000


    );

    public static void main(String[] args) throws MPIException, InterruptedException {

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        for (int N : sizes) {

            Matrix A = new Matrix(N, N);
            Matrix B = new Matrix(N, N);
            Matrix C = new Matrix(N, N);

            if (rank == 0) {
                A.fillRandom();
                B.fillRandom();


                if (PRINT_MATRICES) {
                    System.out.println("Matrix A:");
                    A.print();
                    System.out.println("Matrix B:");
                    B.print();
                }
            }

            if (N % size != 0) {
                throw new IllegalArgumentException("Matrix size should be divisible by number of processors");
            }

            /// TASK 1
            final Types[] types = new Types[]{Types.COLLECTIVE};
            /// TASK 2
//            final Types[] types = new Types[]{Types.POINT_TO_POINT, Types.MANY_TO_ONE, Types.MANY_TO_MANY, Types.ONE_TO_MANY};
            long startTime = System.currentTimeMillis();
            for (Types type : types) {
                if (rank == 0)
                    System.out.println("___________");
                type.function(N, rank, PRINT_MATRICES, size, A, B, C);

                if (rank == 0) {
                    final long endTime = System.currentTimeMillis();
                    System.out.println("Type: " + type + " - " + (endTime - startTime) + " ms - N: " + N);


                }

            }


            MPI.COMM_WORLD.Barrier();

        }

        MPI.Finalize();
    }
}