package me.w1992wishes.spark.mllib.dbscan;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class DBSCANClustering implements Serializable {
	private static final long serialVersionUID = -974892929806170781L;

	// 半径
	private double Epislon = 0.875;

	// 密度、最小点个数
	private int MinPts = 3;

	public DBSCANClustering() {
		
	}

	public DBSCANClustering(double epislon, int minPts) {
		this.Epislon = epislon;
		this.MinPts = minPts;
	}

	/**
	 * @return
	 */
	public List<DataPoint> randomData(int count, int dimension) {
		List<DataPoint> points = new LinkedList<>();
		Random random = new Random();
		try {
			for (int j = 0; j < count; j++) {
				double[] face = new double[dimension];
				for (int i = 0; i < dimension; i++) {
					face[i] = RandomUtils.nextDouble(random);
				}
				points.add(new DataPoint(face, "dd=" + j, false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return points;
	}

	/**
	 * 
	 * @param filePath
	 *            "D:\\intellifdata\\f2w.csv"
	 * @return
	 */
	public List<DataPoint> initData(String filePath) {
		List<DataPoint> points = new LinkedList<>();
		InputStream in = null;
		BufferedReader br = null;

		try {
			in = new FileInputStream(filePath);
			br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();

			int j = 0;
			while (null != line && !"".equals(line)) {
				StringTokenizer tokenizer = new StringTokenizer(line, "_");
				double[] dimensioin = new double[tokenizer.countTokens()];
				int i = 0;
				while (tokenizer.hasMoreTokens()) {
					dimensioin[i] = Double.parseDouble(tokenizer.nextToken());
					i++;
				}

				points.add(new DataPoint(dimensioin, "dd=" + j, false));
				j++;
				line = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(br);
		}
		return points;
	}

	// 计算两点之间的欧氏距离
	private double euclideanDistance(DataPoint a, DataPoint b) {
		double[] dim1 = a.getDimensioin();
		double[] dim2 = b.getDimensioin();
		double distance = 0.0;
		if (dim1.length == dim2.length) {
			for (int i = 0; i < dim1.length; i++) {
				double temp = Math.pow((dim1[i] - dim2[i]), 2);
				distance = distance + temp;
			}
			distance = Math.pow(distance, 0.5);
			return distance;
		}
		return distance;
	}

	/**
	 * 
	 * @param uri
	 *            hdfs://bigdataserver1:8020
	 * @param fileName
	 *            /clusterR/xxxxx.xxx
	 * @param content
	 * @param user
	 *            hdfs
	 */
	public void printResultToHDFS(String uri, String fileName, String content, String user) {
		try {
			Configuration conf = new Configuration();
			FileSystem fs = FileSystem.get(new URI(uri), conf, user);
			FSDataOutputStream in = fs.create(new Path(fileName));
			in.writeBytes(content);
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取当前点的邻居
	private List<DataPoint> obtainNeighbors(DataPoint current, List<DataPoint> points) {
		List<DataPoint> neighbors = new LinkedList<>();
		for (DataPoint point : points) {
			double distance = euclideanDistance(current, point);
			if (distance <= Epislon) {
				neighbors.add(point);
			}
		}
		return neighbors;
	}

	private void mergeCluster(DataPoint point, List<DataPoint> neighbors, int clusterId, List<DataPoint> points) {
		point.setClusterId(clusterId);

		while (!neighbors.isEmpty()) {
			DataPoint neighbor = neighbors.get(0);
			if (!neighbor.isVisited()) {
				neighbor.setVisited(true);
				List<DataPoint> nneighbors = obtainNeighbors(neighbor, points);
				if (nneighbors.size() > MinPts) {
					for (DataPoint nneighbor : nneighbors) {
						if (!neighbors.contains(nneighbor)) {
							neighbors.add(nneighbor);
						}
					}
				}
			}
			// 未被聚类的点归入当前聚类中
			if (neighbor.getClusterId() <= 0) {
				neighbor.setClusterId(clusterId);
			}
			neighbors.remove(neighbor);
		}
	}

	public void cluster(List<DataPoint> points) {
		// clusterId初始为0表示未分类,分类后设置为一个正数,如果设置为-1表示噪声
		int clusterId = 0;
		// 遍历所有的点
		for (DataPoint point : points) {
			// 遍历过就跳过
			if (point.isVisited()) {
				continue;
			}
			point.setVisited(true);
			// 找到其邻域所有点
			List<DataPoint> neighbors = obtainNeighbors(point, points);
			// 邻域中的点大于MinPts，则为核心点
			if (neighbors.size() >= MinPts) {
				// 满足核心对象条件的点创建一个新簇
				clusterId = point.getClusterId() <= 0 ? (++clusterId) : point.getClusterId();
				// 处理该簇内所有未被标记为已访问的点，对簇进行扩展
				mergeCluster(point, neighbors, clusterId, points);
			} else {
				// 未满足核心对象条件的点暂时当作噪声处理
				if (point.getClusterId() <= 0) {
					point.setClusterId(-1);
				}
			}
		}
	}

}