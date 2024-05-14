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
                oneToMany(N, rank, print, size, A, B);
                break;
            case MANY_TO_MANY:
                manyToMany(N, rank, print, size, A, B);
                break;
            case MANY_TO_ONE:
                manyToOne(N, rank, print, size, A, B, C);
                break;
            case COLLECTIVE:
                collective(N, rank, print, size, A, B, C);
                break;
        }
    }

    private void pointToPoint(int N, int rank, boolean print, int size, Matrix a, Matrix b) {
        final double[] c = new double[N * N];
        PointToPoint.multiply(rank, size, N, a, b, c);
        final Matrix C = new Matrix(N, N);
        C.fromArray(c);
        checkResult(rank, print, C, a, b);
    }

    private void oneToMany(int n, int rank, boolean print, int size, Matrix a, Matrix b) {
        final double[] A = a.toArray();
        final double[] B = b.toArray();
        double[] C = new double[n * n];
        int rowsPerProcess = n / size;

        if (rank == 0) {
            for (int dest = 1; dest < size; dest++) {
                MPI.COMM_WORLD.Send(A, dest * rowsPerProcess * n, rowsPerProcess * n, MPI.DOUBLE, dest, 0);
            }
        } else {
            MPI.COMM_WORLD.Recv(A, rank * rowsPerProcess * n, rowsPerProcess * n, MPI.DOUBLE, 0, 0);
        }
        for (int source = 0; source < size; source++) {
            if (rank == source) {
                MPI.COMM_WORLD.Send(B, 0, n * n, MPI.DOUBLE, (rank + 1) % size, 0);
            } else if (rank == (source + 1) % size) {
                MPI.COMM_WORLD.Recv(B, 0, n * n, MPI.DOUBLE, source, 0);
            }
            MPI.COMM_WORLD.Barrier();
        }

        for (int i = rank * rowsPerProcess; i < (rank + 1) * rowsPerProcess; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    C[i * n + j] += A[i * n + k] * B[k * n + j];
                }
            }
        }
        if (rank == 0) {
            for (int source = 1; source < size; source++) {
                MPI.COMM_WORLD.Recv(C, source * rowsPerProcess * n, rowsPerProcess * n, MPI.DOUBLE, source, 0);
            }
        } else {
            MPI.COMM_WORLD.Send(C, rank * rowsPerProcess * n, rowsPerProcess * n, MPI.DOUBLE, 0, 0);
        }

        final Matrix cc = new Matrix(n, n);
        cc.fromArray(C);
        checkResult(rank, print, cc, a, b);
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

        Matrix C = new Matrix(n, n);
        C.fromArray(gatherBuffer);

        checkResult(rank, print, C, A, B);
    }

    private void manyToOne(int N, int rank, boolean print, int size, Matrix A, Matrix B, Matrix C) {

        int rowsPerProcess = N / size;
        double[] c = new double[N * N];

        for (int i = rank * rowsPerProcess; i < (rank + 1) * rowsPerProcess; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    c[i * N + j] += A.matrix[i][k] * B.matrix[k][j];
                }
            }
        }

        if (rank != 0) {
            MPI.COMM_WORLD.Send(c, 0, c.length, MPI.DOUBLE, 0, 0);
        } else {
            for (int i = 1; i < size; i++) {
                double[] temp = new double[N * N];
                MPI.COMM_WORLD.Recv(temp, 0, temp.length, MPI.DOUBLE, i, 0);
                for (int j = 0; j < temp.length; j++) {
                    c[j] += temp[j];
                }
            }

        }
        checkResult(rank, print, C, A, B);
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
            }
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