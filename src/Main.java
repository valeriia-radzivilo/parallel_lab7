//import mpi.MPI;
//import mpi.MPIException;
//
//public class Main {
//
//    static final boolean PRINT_MATRICES = true;
//
//    private static void oneToOne(Matrix A, Matrix B, int N, int rank) throws MPIException {
//        int size = MPI.COMM_WORLD.Size();
//
//        // Calculate the range of rows for this process
//        int from = rank * N / size;
//        int to = (rank + 1) * N / size;
//
//        // Create a new matrix to store the result
//        Matrix C = new Matrix(N, N);
//
//        // Start the timer
//        long startTime = System.currentTimeMillis();
//
//        // If this is the master process
//        if (rank == 0) {
//            // Send each process its portion of matrix A
//            for (int i = 1; i < size; i++) {
//                int startRow = i * N / size;
//                int endRow = (i + 1) * N / size;
//                for (int row = startRow; row < endRow; row++) {
//                    MPI.COMM_WORLD.Send(A.matrix[row], 0, N, MPI.DOUBLE, i, 0);
//                }
//            }
//
//            // The master process also computes its own portion of the result
//            for (int i = from; i < to; i++) {
//                for (int j = 0; j < N; j++) {
//                    for (int k = 0; k < N; k++) {
//                        C.add(i - from, j, A.matrix[i][k] * B.matrix[k][j]);
//                    }
//                }
//            }
//        } else {
//            // If this is a worker process
//            // Receive the portion of matrix A from the master process
//            for (int i = 0; i < to - from; i++) {
//                MPI.COMM_WORLD.Recv(A.matrix[i], 0, N, MPI.DOUBLE, 0, 0);
//            }
//
//            // Compute the result
//            for (int i = 0; i < to - from; i++) {
//                for (int j = 0; j < N; j++) {
//                    for (int k = 0; k < N; k++) {
//                        C.add(i, j, A.matrix[i][k] * B.matrix[k][j]);
//                    }
//                }
//            }
//
//            // Send the result back to the master process
//            for (int i = 0; i < to - from; i++) {
//                MPI.COMM_WORLD.Send(C.matrix[i], 0, N, MPI.DOUBLE, 0, 0);
//            }
//        }
//
//        // If this is the master process
//        if (rank == 0) {
//            // Receive the results from the worker processes
//            for (int i = 1; i < size; i++) {
//                int startRow = i * N / size;
//                int endRow = (i + 1) * N / size;
//                for (int row = startRow; row < endRow; row++) {
//                    MPI.COMM_WORLD.Recv(C.matrix[row], 0, N, MPI.DOUBLE, i, 0);
//                }
//            }
//
//            // Stop the timer
//            long endTime = System.currentTimeMillis();
//
//            // Print the result and the time taken
//            if (PRINT_MATRICES) {
//                System.out.println("Result:");
//                C.print();
//            }
//            System.out.println("Time: " + (endTime - startTime) + " ms");
//        }
//    }
//
//    private static void oneToMany(Matrix A, Matrix B, int N, int rank) throws MPIException {
//        int size = MPI.COMM_WORLD.Size();
//
//        // Calculate the range of rows for this process
//        int from = rank * N / size;
//        int to = (rank + 1) * N / size;
//
//        // Create a new matrix to store the result
//        Matrix C = new Matrix(N, N);
//
//        // Start the timer
//        long startTime = System.currentTimeMillis();
//
//        // Broadcast matrix B to all processes
//        MPI.COMM_WORLD.Bcast(B.matrix, 0, N, MPI.OBJECT, 0);
//
//        // If this is the master process
//        if (rank == 0) {
//            // Send each process its portion of matrix A
//            for (int i = 1; i < size; i++) {
//                int startRow = i * N / size;
//                int endRow = (i + 1) * N / size;
//                for (int row = startRow; row < endRow; row++) {
//                    MPI.COMM_WORLD.Send(A.matrix[row], 0, N, MPI.DOUBLE, i, 0);
//                }
//            }
//
//            // The master process also computes its own portion of the result
//            for (int i = from; i < to; i++) {
//                for (int j = 0; j < N; j++) {
//                    for (int k = 0; k < N; k++) {
//                        C.add(i - from, j, A.matrix[i][k] * B.matrix[k][j]);
//                    }
//                }
//            }
//        } else {
//            // If this is a worker process
//            // Receive the portion of matrix A from the master process
//            for (int i = 0; i < to - from; i++) {
//                MPI.COMM_WORLD.Recv(A.matrix[i], 0, N, MPI.DOUBLE, 0, 0);
//            }
//
//            // Compute the result
//            for (int i = 0; i < to - from; i++) {
//                for (int j = 0; j < N; j++) {
//                    for (int k = 0; k < N; k++) {
//                        C.add(i, j, A.matrix[i][k] * B.matrix[k][j]);
//                    }
//                }
//            }
//
//            // Send the result back to the master process
//            for (int i = 0; i < to - from; i++) {
//                MPI.COMM_WORLD.Send(C.matrix[i], 0, N, MPI.DOUBLE, 0, 0);
//            }
//        }
//
//        // If this is the master process
//        if (rank == 0) {
//            // Receive the results from the worker processes
//            for (int i = 1; i < size; i++) {
//                int startRow = i * N / size;
//                int endRow = (i + 1) * N / size;
//                for (int row = startRow; row < endRow; row++) {
//                    MPI.COMM_WORLD.Recv(C.matrix[row], 0, N, MPI.DOUBLE, i, 0);
//                }
//            }
//
//            // Stop the timer
//            long endTime = System.currentTimeMillis();
//
//            // Print the result and the time taken
//            if (PRINT_MATRICES) {
//                System.out.println("Result:");
//                C.print();
//            }
//            System.out.println("Time: " + (endTime - startTime) + " ms");
//        }
//    }
//
//    public static void main(String[] args) throws MPIException {
//        MPI.Init(args);
//
//        int rank = MPI.COMM_WORLD.Rank();
//        int size = MPI.COMM_WORLD.Size();
//
//        int N = 4;
//
//        if (N % size != 0) {
//            throw new IllegalArgumentException("Matrix size should be divisible by number of processors");
//        }
//
//        Matrix A = Matrix.generateRandom(N, N);
//        Matrix B = Matrix.generateRandom(N, N);
//        double[] C = new double[N];
//
//        if (rank == 0 && PRINT_MATRICES) {
//            System.out.println("Matrix A:");
//            A.print();
//            System.out.println("Matrix B:");
//            B.print();
//        }
//
//        oneToOne(A, B, N, rank);
//
//
//        MPI.Finalize();
//    }
//}