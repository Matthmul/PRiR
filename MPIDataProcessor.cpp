/*
 * MPIDataProcessor.cpp
 */

#include "MPIDataProcessor.h"
#include "Alloc.h"
#include <iostream>
#include <math.h>
#include "mpi.h" 

#define TAG 0

void MPIDataProcessor::shareData() 
{
	int rank;
    int procNum;
    MPI_Status status;
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &procNum);

	if (rank != MAIN_PROC_ID) 
    {
		MPI_Recv(&tabSize, TAB_ARRAY_SIZE, MPI_INT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD, &status);
        data = tableAlloc(tabSize);
		for (int i = 0; i < tabSize[ROW]; ++i) 
		{
			MPI_Recv(*(data + i), dataPortionSize, MPI_FLOAT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD, &status);
		}
	}
    else 
    {
		calcDataToDivine(procNum);
		
        for (auto num = 1; num < procNum; ++num)
        {
			MPI_Send(&tabSize, TAB_ARRAY_SIZE, MPI_INT, num, TAG, MPI_COMM_WORLD);
			for (int i = 0; i < tabSize[ROW]; ++i) 
			{
            	MPI_Send(*(data + i), dataPortionSize, MPI_FLOAT, num, TAG, MPI_COMM_WORLD);
			}
			data = data + tabSize[ROW] - (margin * 2);
        }
		tabSize[ROW] += dataSize % procNum;
    }

	nextData = tableAlloc(tabSize);
	for (int i = 0; i < tabSize[ROW]; ++i)
		for (int j = 0; j < tabSize[COL]; ++j)
		{
			nextData[i][j] = data[i][j];
		}

}

void MPIDataProcessor::calcDataToDivine(int procNum) 
{
	tabSize[ROW] = (dataSize - (margin * 2)) / procNum + (margin * 2); // margin from both sides
	tabSize[COL] = dataSize;
}

double **MPIDataProcessor::tableAlloc( int *tabSize ) 
{
	double **result;
	result = new double* [ tabSize[ROW] ]; // size rows
	for ( int i = 0; i < tabSize[ROW]; ++i )
		result[ i ] = new double[ tabSize[COL] ]; // size cols
	return result;
}

void MPIDataProcessor::singleExecution() 
{
	double *buffer = new double[dataPortionSize];
	for (int row = margin; row < tabSize[ROW] - margin; ++row)
		for (int col = margin; col < tabSize[COL] - margin; ++col) {
			createDataPortion(row, col, buffer);
			nextData[row][col] = function->calc(buffer);

		}
	delete[] buffer;
    double **tmp = data;
	data = nextData;
	nextData = tmp;
}

void MPIDataProcessor::collectData()
{
	int rank;
    int procNum;
    MPI_Status status;
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &procNum);

	if (rank != MAIN_PROC_ID) 
    {
		for (int i = 0; i < tabSize[ROW]; i++) 
		{
			MPI_Send(*(data + i), dataPortionSize, MPI_FLOAT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD);
		}
	}
    else 
    {
		int tmpTabSize[TAB_ARRAY_SIZE] = {dataSize, dataSize};
		double **tmp = tableAlloc(tmpTabSize);
		tabSize[ROW] -= dataSize % procNum;
        for (auto num = 1; num < procNum; ++num)
        {
			double **tabForProc = tmp + ((num - 1) * tabSize[ROW]);
			for (int i = 0; i < tabSize[ROW]; i++) 
			{
            	MPI_Recv(*(tabForProc + i), dataPortionSize, MPI_FLOAT, num, TAG, MPI_COMM_WORLD, &status);
			}
        }

		tabSize[ROW] += dataSize % procNum;
		double **tabForProc = tmp + ((procNum - 1) * tabSize[ROW] - (margin * 2));
		for (int i = 0; i < tabSize[ROW]; ++i)
			for (int j = 0; j < tabSize[COL]; ++j)
			{
				tabForProc[i][j] = data[i][j];
			}
		data = tmp;
    }
}

void MPIDataProcessor::createDataPortion(int row, int col,
		double *buffer) 
{
	int counter = 0;
	for (int i = row - margin; i <= row + margin; i++)
		for (int j = col - margin; j <= col + margin; j++)
			buffer[counter++] = data[i][j];
}
