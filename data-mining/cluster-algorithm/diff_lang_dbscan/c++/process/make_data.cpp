#include<stdio.h>
#include<time.h>
#include<cstdlib>
#define N 1000000
#define CN 30
#define Max 10000000
#define ptsize 500

struct pt
{
	int x[3];
};

int getrand(int M)
{
	int t1 = (rand() % 0x7fff) << 16;
	return (t1 + rand() % 0x7fff) % M;
}

pt cluster[100];
pt point[1000000];
int main()
{
	freopen("data.txt", "w", stdout);
	srand((unsigned)time(NULL));
	int perCl = int((N*0.8) / CN);
	for (int i = 0; i<CN; ++i)
	{
		cluster[i].x[0] = getrand(Max);
		cluster[i].x[1] = getrand(Max);
		cluster[i].x[2] = getrand(Max);
	}
	for (int i = 0; i<CN - 1; ++i)
		for (int j = 0; j<perCl; ++j)
		{
			point[i*perCl + j].x[0] = (cluster[i].x[0] + getrand(ptsize)) % Max;
			point[i*perCl + j].x[1] = (cluster[i].x[1] + getrand(ptsize)) % Max;
			point[i*perCl + j].x[2] = (cluster[i].x[2] + getrand(ptsize)) % Max;
		}
	for (int i = perCl * (CN - 1); i<perCl*CN; ++i)
	{
		point[i].x[0] = getrand(ptsize);
		point[i].x[1] = point[i].x[0] * 5 + 7;
		point[i].x[2] = point[i].x[0] * 5 + 7;
	}
	for (int i = perCl * CN; i<N; ++i)
	{
		point[i].x[0] = getrand(Max);
		point[i].x[1] = getrand(Max);
		point[i].x[2] = getrand(Max);
	}
	printf("%d\n", N);
	for (int i = 0; i<N; ++i)
		printf("%d %d %d\n", point[i].x[0], point[i].x[1], point[i].x[2]);

}
