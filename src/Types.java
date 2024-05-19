import mpi.MPI;

public enum Types {

    POINT_TO_POINT,
    ONE_TO_MANY,
    MANY_TO_MANY,

    MANY_TO_ONE,
    COLLECTIVE;

    public String toString() {
        return switch (this) {
            case POINT_TO_POINT -> "POINT_TO_POINT";
            case ONE_TO_MANY -> "ONE_TO_MANY";
            case MANY_TO_MANY -> "MANY_TO_MANY";
            case MANY_TO_ONE -> "MANY_TO_ONE";
            case COLLECTIVE -> "COLLECTIVE";
        };
    }


    public void function(int N, int rank, boolean print, int size, Matrix A, Matrix B, Matrix C) {
        switch (this) {
            case POINT_TO_POINT:
                pointToPoint(N, rank, print, size, A, B);
                break;
            case ONE_TO_MANY:
                collective(N, rank, print, size, A, B, C);
                break;
            case MANY_TO_MANY:
                manyToMany(N, rank, print, size, A, B);
                break;
            case MANY_TO_ONE:
                manyToOne(N, rank, print, size, A, B);
                break;
            case COLLECTIVE:
                collective(N, rank, print, size, A, B, C);
                break;
        }
        MPI.COMM_WORLD.Barrier();
    }

    private void pointToPoint(int N, int rank, boolean print, int size, Matrix a, Matrix b) {
        final double[] c = new double[N * N];
        PointToPoint.multiply(rank, size, N, a, b, c);
        MPI.COMM_WORLD.Barrier();
        if (rank == 0) {
            final Matrix C = new Matrix(N, N);
            C.fromArray(c);
            checkResult(rank, print, C, a, b);
        }
    }


    private void manyToMany(int n, int rank, boolean print, int size, Matrix A, Matrix B) {
        double[] a = A.toArray();
        double[] b = B.toArray();
        int rowsPerProcess = n / size;
        double[] c = new double[rowsPerProcess * n];

        for (int i = 0; i < rowsPerProcess; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    c[i * n + j] += a[(rank * rowsPerProcess + i) * n + k] * b[k * n + j];
                }
            }
        }

        double[] gatherBuffer = new double[n * n];
        MPI.COMM_WORLD.Allgather(c, 0, c.length, MPI.DOUBLE, gatherBuffer, rank * rowsPerProcess * n, c.length, MPI.DOUBLE);

        if (rank == 0) {
            Matrix C = new Matrix(n, n);
            C.fromArray(gatherBuffer);

            checkResult(rank, print, C, A, B);
        }
    }

    private void manyToOne(int n, int rank, boolean print, int size, Matrix A, Matrix B) {
        int[] offset = new int[size];
        int[] rows = new int[size];
        double[][] result_matrix = new double[n][n];

        int amount_for_process = n / size;
        int extra = n % size;

        for (int i = 0; i < size; i++) {
            rows[i] = i < extra ? amount_for_process + 1 : amount_for_process;
            offset[i] = i == 0 ? 0 : offset[i - 1] + rows[i - 1];
        }

        int local_a_rows = rows[rank];
        double[][] local_a = new double[local_a_rows][n];

        MPI.COMM_WORLD.Scatterv(A.matrix, 0, rows, offset, MPI.OBJECT,
                local_a, 0, local_a_rows, MPI.OBJECT, 0
        );

        MPI.COMM_WORLD.Bcast(B.matrix, 0, n, MPI.OBJECT, 0);

        double[][] local_result_matrix_rows = new double[local_a_rows][n];
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < local_a_rows; i++) {
                for (int j = 0; j < n; j++) {
                    local_result_matrix_rows[i][k] += local_a[i][j] * B.matrix[j][k];
                }
            }
        }

        MPI.COMM_WORLD.Gatherv(local_result_matrix_rows, 0, local_a_rows, MPI.OBJECT,
                result_matrix, 0, rows, offset, MPI.OBJECT,
                0
        );
        if (rank == 0) {
            Matrix C = new Matrix(result_matrix);
            checkResult(rank, print, C, A, B);
        }

    }

    private void collective(int N, int rank, boolean print, int size, Matrix A, Matrix B, Matrix C) {


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


        checkResult(rank, print, C, A, B);
    }


    private void checkResult(int rank, boolean print, Matrix C, Matrix A, Matrix B) {
        if (rank == 0) {
            if (print) {
                System.out.println("Result:");
                C.print();
                // Check result
                Matrix expected = A.multiply(B);
                if (C.equals(expected)) {
                    System.out.println("Results are correct!");
                } else {
                    System.out.println("Error in the result");
                }
            }

        }
    }
}