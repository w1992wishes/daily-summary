#define sqr(x) ((x)*(x))
struct pt
{
	float x[128];
};
// para: 0:n 1:esp 2:maxM 3:cor 4:flag 5:lasttag 6:rectag
__kernel void bfs(__global struct pt* point,
	__global int *pre,__global int *next,
	__global int *tags,__global bool *core,__global int  *cor, __global int *para, __global float *eps)
{
	int tn=get_local_size(0);
	int gn=get_num_groups(0);
	int x=get_group_id(0);//返回各个工作项的一维全局ID
	int y=get_local_id(0);

	int py,tpx;
	float tmp;

	local int px;


	if (y==0)
	{
		px=x;
		while ((tags[px]!=para[5])&&(px<para[0])) px+=gn;
		cor[px]=para[3];
	}
	barrier(CLK_LOCAL_MEM_FENCE);

	while (px<para[0])
	{
		py=pre[px]+y;
		while (py<=next[px])
		{
			int dim;
			tmp = 0.0f;
			for (dim = 0; dim < 128; dim++) {
				tmp += sqr(point[px].x[dim] - point[py].x[dim]);
			}
			if (tmp<=(*eps)*(*eps))
			{
				if (cor[py]==0) cor[py]=para[3];
				if ((tags[py]==0)&&(core[py])) {tags[py]=para[6]; para[4]=1;}
			}
			py+=tn;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		if (y==0)
		{
			px+=gn;
			while ((tags[px]!=para[5])&&(px<para[0])) px+=gn;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
	}
}

// para: 0:n 1:esp 2:maxM 3:cor 4:flag
__kernel void core(__global struct pt* point,
	__global int* pre,__global int *next,
	__global bool* core, __global int* para, __global float *eps)
{
	int gn=get_global_size(0);
	int i,cnt;
	float tmp;
	int px=get_global_id(0);
	while (px<para[0])
	{
		cnt=1;
		for (i=pre[px];i<=next[px];++i)
		{
			int dim;
			tmp = 0.0f;
			for (dim = 0; dim < 128; dim++) {
				tmp += sqr(point[px].x[dim] - point[i].x[dim]);
			}
			if (tmp<=(*eps)*(*eps)) cnt++;
		}
		if (cnt>para[2]) core[px]=true; else core[px]=false;
		px+=gn;
	}
}

// para: 0:n 1:esp 2:maxM 3:cor 4:flag 5:lasttag 6:rectag 7:set_st
__kernel void set(__global struct pt* point,
	__global int *para,
	__global int *tags,__global bool* core)
{
	para[4]=0;
	for (int i=para[7];i<para[0];++i)
	if ((core[i])&&(tags[i]==0))
	{
		tags[i]=para[6];
		para[4]=1; para[8]=i; para[9]=core[i];
		break;
	}
}



