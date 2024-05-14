import mpi.MPI;

public class PointToPoint {

    public static void multiply(int rank, int numberOfProcessors, int n, Matrix A, Matrix B, double[] C) {
        int rowsPerProcess = n / numberOfProcessors;

        if (rank == 0) {
            sendMatricesToWorkers(numberOfProcessors, n, A);
        } else {
            receiveMatrixFromMaster(rank, n, A);
        }

        exchangeMatrixBetweenProcesses(rank, numberOfProcessors, n, B);

        // Perform matrix multiplication for assigned rows
        computeMatrixMultiplication(rank, rowsPerProcess, n, A, B, C);

        if (rank == 0) {
            collectResultsFromWorkers(numberOfProcessors, n, C);
        } else {
            sendResultToMaster(rank, rowsPerProcess, n, C);
        }
    }

    private static void sendMatricesToWorkers(int numberOfProcessors, int n, Matrix A) {
        for (int dest = 1; dest < numberOfProcessors; dest++) {
            MPI.COMM_WORLD.Send(A.toArray(), dest * (n / numberOfProcessors) * n, (n / numberOfProcessors) * n, MPI.DOUBLE, dest, 0);
        }
    }

    private static void receiveMatrixFromMaster(int rank, int n, Matrix A) {
        MPI.COMM_WORLD.Recv(A.toArray(), rank * (n / MPI.COMM_WORLD.Size()) * n, (n / MPI.COMM_WORLD.Size()) * n, MPI.DOUBLE, 0, 0);
    }

    private static void exchangeMatrixBetweenProcesses(int rank, int numberOfProcessors, int n, Matrix B) {
        for (int source = 0; source < numberOfProcessors; source++) {
            if (rank == source) {
                MPI.COMM_WORLD.Send(B.toArray(), 0, n * n, MPI.DOUBLE, (rank + 1) % numberOfProcessors, 0);
            } else if (rank == (source + 1) % numberOfProcessors) {
                MPI.COMM_WORLD.Recv(B.toArray(), 0, n * n, MPI.DOUBLE, source, 0);
            }
        }
    }

    private static void computeMatrixMultiplication(int rank, int rowsPerProcess, int n, Matrix A, Matrix B, double[] C) {
        for (int i = rank * rowsPerProcess; i < (rank + 1) * rowsPerProcess; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    C[i * n + j] += A.matrix[i][k] * B.matrix[k][j];
                }
            }
        }
    }

    private static void collectResultsFromWorkers(int numberOfProcessors, int n, double[] C) {
        for (int source = 1; source < numberOfProcessors; source++) {
            MPI.COMM_WORLD.Recv(C, source * (n / numberOfProcessors) * n, (n / numberOfProcessors) * n, MPI.DOUBLE, source, 0);
        }
    }

    private static void sendResultToMaster(int rank, int rowsPerProcess, int n, double[] C) {
        MPI.COMM_WORLD.Send(C, rank * (n / MPI.COMM_WORLD.Size()) * n, (n / MPI.COMM_WORLD.Size()) * n, MPI.DOUBLE, 0, 0);
    }
}