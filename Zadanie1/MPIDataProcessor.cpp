/*
 * MPIDataProcessor.cpp
 */

#include "MPIDataProcessor.h"

#define TAG 0

MPIDataProcessor::MPIDataProcessor() 
{
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &procNum);
}

void MPIDataProcessor::shareData() 
{
	if (rank != MAIN_PROC_ID) 
    {
		MPI_Recv(&tabSize, TAB_ARRAY_SIZE, MPI_INT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD, &status);
        data = tableAlloc(tabSize);

		for (int i = 0; i < tabSize[ROW]; ++i) 
		{
			MPI_Recv(*(data + i), tabSize[COL] * 2, MPI_FLOAT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD, &status);
		}
	}
    else 
    {
        for (auto num = procNum - 1; num > 0; --num)
        {
			calcTabSize(procNum, num);
			MPI_Send(&tabSize, TAB_ARRAY_SIZE, MPI_INT, num, TAG, MPI_COMM_WORLD);
			for (int i = 0; i < tabSize[ROW]; ++i) 
			{
            	MPI_Send(*(data + i), tabSize[COL] * 2, MPI_FLOAT, num, TAG, MPI_COMM_WORLD);
			}
			data += tabSize[ROW] - getMargin();
        }
		calcTabSize(procNum, MAIN_PROC_ID);
    }

	nextData = tableAlloc(tabSize);
	for (int i = 0; i < tabSize[ROW]; ++i)
		for (int j = 0; j < tabSize[COL]; ++j)
		{
			nextData[i][j] = data[i][j];
		}
}

void MPIDataProcessor::calcTabSize(int maxProcNum, int procNum) 
{
	tabSize[ROW] = (dataSize - getMargin()) / maxProcNum + getMargin(); // margin from both sides
	int leftRows = (dataSize - getMargin()) % maxProcNum;
	if (procNum != MAIN_PROC_ID && leftRows != 0 && procNum <= leftRows)
	{
		tabSize[ROW] += 1;
	}
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
	syncData();

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

void MPIDataProcessor::sendTopMargin( int dest )
{
	if (rank != procNum - 1)
	{
		for (int i = margin; i < getMargin(); ++i) 
		{
			MPI_Send(*(data + i), tabSize[COL] * 2, MPI_FLOAT, dest, TAG, MPI_COMM_WORLD);
		}
	}
}

void MPIDataProcessor::sendDownMargin( int dest )
{
	if (rank != MAIN_PROC_ID)
	{
		for (int i = tabSize[ROW] - getMargin(); i < tabSize[ROW] - margin; ++i) 
		{
			MPI_Send(*(data + i), tabSize[COL] * 2, MPI_FLOAT, dest, TAG, MPI_COMM_WORLD);
		}
	}
}

void MPIDataProcessor::recvTopMargin( int dest )
{
	if (rank != procNum - 1)
	{
		for (int i = 0; i < margin; ++i) 
		{
			MPI_Recv(*(data + i), tabSize[COL] * 2, MPI_FLOAT,  dest, TAG, MPI_COMM_WORLD, &status);
		}
	}
}

void MPIDataProcessor::recvDownMargin( int dest )
{
	if (rank != MAIN_PROC_ID)
	{
		for (int i = tabSize[ROW] - margin; i < tabSize[ROW]; ++i)
		{
			MPI_Recv(*(data + i), tabSize[COL] * 2, MPI_FLOAT, dest, TAG, MPI_COMM_WORLD, &status);
		}
	}
}

void MPIDataProcessor::syncData()
{
	switch (rank)
	{
	case MAIN_PROC_ID:
		recvTopMargin(rank + 1);
		sendTopMargin(rank + 1);
		break;
	default:
		if (rank == procNum-1)
		{
			if (procNum % 2 == 0)
			{
				sendDownMargin(rank - 1);
				recvDownMargin(rank - 1);
			}
			else
			{
				recvDownMargin(rank - 1);
				sendDownMargin(rank - 1);
			}
		}
		else
		{
			switch(rank%2)
			{
			case 0:
				recvTopMargin(rank + 1);
				recvDownMargin(rank - 1);
				sendTopMargin(rank + 1);
				sendDownMargin(rank - 1);
				break;
			case 1:
				sendTopMargin(rank + 1);
				sendDownMargin(rank - 1);
				recvTopMargin(rank + 1);
				recvDownMargin(rank - 1);
				break;
			}
		}
		break;
	}
}

void MPIDataProcessor::collectData()
{
	if (rank != MAIN_PROC_ID) 
    {
		int i = (rank == (procNum - 1)) ? 0 : margin;
		for (; i < tabSize[ROW] - margin; ++i) 
		{
			MPI_Send(*(data + i), tabSize[COL] * 2, MPI_FLOAT, MAIN_PROC_ID, TAG, MPI_COMM_WORLD);
		}
	}
    else 
    {
		int tmpTabSize[TAB_ARRAY_SIZE] = {dataSize, dataSize};
		double **nextData = tableAlloc(tmpTabSize);

		double **tmp = nextData;
        for (auto num = procNum - 1; num > 0; --num)
        {
			calcTabSize(procNum, num);
			int i = (num == (procNum - 1)) ? 0 : margin;
			for (; i < tabSize[ROW] - margin; ++i) 
			{
            	MPI_Recv(*(tmp + i), tabSize[COL] * 2, MPI_FLOAT, num, TAG, MPI_COMM_WORLD, &status);
			}
			tmp += tabSize[ROW] - getMargin();
        }

		calcTabSize(procNum, MAIN_PROC_ID);
		for (int i = margin; i < tabSize[ROW]; ++i)
		{
			for (int j = 0; j < tabSize[COL]; ++j)
			{
				tmp[i][j] = data[i][j];
			}
		}
		data = nextData;
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
