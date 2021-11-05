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
	const int REPETITIONS = 3;

	int dataPortion = calcDataPortion(MARGIN);
	MagicFuntion *mf = new ArithmeticMeanFunction(dataPortion);
	DataProcessor *dp = new MPIDataProcessor();
	dp->setMagicFunction(mf);

	if (rank == 0) {
		double **initialData = tableAlloc(DATA_SIZE);
		InitialDataGenerator *generator = new SimpleInitialDataGenerator(1, 10);
		generator->fillWithData(initialData, DATA_SIZE, MARGIN);
		// showTable(initialData, DATA_SIZE);

		dp->setInitialData(initialData, DATA_SIZE);
	}

	dp->execute(REPETITIONS);

	if (rank == 0) {
		double **result = dp->getResult();
		showTable(result, DATA_SIZE);
	}

	MPI_Finalize();

	return 0;
}

namespace {
           
//  if (size > 1) {                        // sprawdzane jest czy istnieje minimalna liczba procesów

//    // Komunikat przesyłany jest od procesu o numerze rank = 0 (SOURCE)
//    // do procesu o numerze rank = 1 (DEST) 
//    if (rank == DEST)  // jeżeli procesem jest proces odbierający komunikat:                     
//      {
//        printf ("process %d of %d waiting for message from %d\n",
//                rank, size, SOURCE);
//        //  odbiór komunkatu i zapis zawartości do bufora Recv manual 
//        MPI_Recv(Recv, COUNT, MPI_CHAR, SOURCE, TAG, MPI_COMM_WORLD, &status);        
//        printf ("process %d of %d has received: '%s'\n", 
//                rank, size, Recv);
//      }
   
// else

//      if (rank == SOURCE)  // jeżeli procesem jest proces nadający komunikat: 
//        {
//          printf ("process %d of %d sending '%s' to %d\n",
//                  rank, size, Msg, DEST);
//          //  wysyłanie komunkatu zawartego w buforze Msg manual
//          MPI_Send(Msg, COUNT, MPI_CHAR, DEST, TAG, MPI_COMM_WORLD); 
//        }
 
//  }
}
