#include <math.h> 
#include <iostream>
#include <iomanip>
#include <limits>
#include "mpi.h" 
#include "DataProcessor.h"
#include "MagicFuntion.h"
#include "ArithmeticMeanFunction.h"
#include "SimpleInitialDataGenerator.h"
#include "SequentialDataProcessor.h"
#include "MPIDataProcessor.h"
#include "Alloc.h"

using namespace std;

int calcDataPortion(int margin) {
	int dataPortion = margin * 2 + 1;
	dataPortion *= dataPortion;
	return dataPortion;
}

void showTable(double **table, int dataSize) {
	cout << "----------------------------------" << endl;
	for (int i = 0; i < dataSize; i++) {
		cout << setw(3) << i << " -> ";
		for (int j = 0; j < dataSize; j++)
			cout << " " << showpoint << setw(4) << setprecision(3)
					<< table[i][j];
		cout << endl;
	}
}

int main(int argc, char *argv[]) {

	MPI_Init(&argc, &argv);
	int rank;
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);

	const int MARGIN = 2;
	const int DATA_SIZE = 12;
	const int REPETITIONS = 10;

	// int dataPortionp = calcDataPortion(MARGIN);
	// MagicFuntion *mfp = new ArithmeticMeanFunction(dataPortionp);
	// DataProcessor *sdp = new SequentialDataProcessor();
	// sdp->setMagicFunction(mfp);

	// if (rank == 0) {
	// 	double **initialDatap = tableAlloc(DATA_SIZE);
	// 	InitialDataGenerator *generatorp = new SimpleInitialDataGenerator(1, 10);
	// 	generatorp->fillWithData(initialDatap, DATA_SIZE, MARGIN);
	// 	// showTable(initialDatap, DATA_SIZE);

	// 	sdp->setInitialData(initialDatap, DATA_SIZE);
	// }

	// sdp->execute(REPETITIONS);

	// if (rank == 0) {
	// 	double **result = sdp->getResult();
	// 	// showTable(result, DATA_SIZE);
	// }

	int dataPortion = calcDataPortion(MARGIN);
	MagicFuntion *mf = new ArithmeticMeanFunction(dataPortion);
	DataProcessor *dp = new MPIDataProcessor();
	dp->setMagicFunction(mf);

	if (rank == 0) {
		double **initialData = tableAlloc(DATA_SIZE);
		InitialDataGenerator *generator = new SimpleInitialDataGenerator(1, 10);
		generator->fillWithData(initialData, DATA_SIZE, MARGIN);
		cout << " start" << endl;
		// showTable(initialData, DATA_SIZE);

		dp->setInitialData(initialData, DATA_SIZE);
	}

	dp->execute(REPETITIONS);

	if (rank == 0) {
		double **result = dp->getResult();
		// double **results = sdp->getResult();
		cout << " stop" << endl;

		// showTable(result, DATA_SIZE);
	
		// for (int i = 0; i < DATA_SIZE; i++) {
		// 	for (int j = 0; j < DATA_SIZE; j++)
		// 			if (result[i][j] != results[i][j])
		// 				cout << " duuupa " << i << " " << j << endl;
		// }
	}

	MPI_Finalize();

	return 0;
}
