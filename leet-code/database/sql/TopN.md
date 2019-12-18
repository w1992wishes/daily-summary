## 部门工资前三高的所有员工

Employee 表包含所有员工信息，每个员工有其对应的工号 Id，姓名 Name，工资 Salary 和部门编号 DepartmentId 。

```
+----+-------+--------+--------------+
| Id | Name  | Salary | DepartmentId |
+----+-------+--------+--------------+
| 1  | Joe   | 85000  | 1            |
| 2  | Henry | 80000  | 2            |
| 3  | Sam   | 60000  | 2            |
| 4  | Max   | 90000  | 1            |
| 5  | Janet | 69000  | 1            |
| 6  | Randy | 85000  | 1            |
| 7  | Will  | 70000  | 1            |
+----+-------+--------+--------------+
```

Department 表包含公司所有部门的信息。

```
+----+----------+
| Id | Name     |
+----+----------+
| 1  | IT       |
| 2  | Sales    |
+----+----------+
```

编写一个 SQL 查询，找出每个部门获得前三高工资的所有员工。例如，根据上述给定的表，查询结果应返回：

```
+------------+----------+--------+
| Department | Employee | Salary |
+------------+----------+--------+
| IT         | Max      | 90000  |
| IT         | Randy    | 85000  |
| IT         | Joe      | 85000  |
| IT         | Will     | 70000  |
| Sales      | Henry    | 80000  |
| Sales      | Sam      | 60000  |
+------------+----------+--------+
```

解释：

IT 部门中，Max 获得了最高的工资，Randy 和 Joe 都拿到了第二高的工资，Will 的工资排第三。销售部门（Sales）只有两名员工，Henry 的工资最高，Sam 的工资排第二。

## 解题

回忆一下 count 函数

```
MySql
count(字段名)  # 返回表中该字段总共有多少条记录
回忆一下 DISTINCT 关键字

MySql
DISTINCT 字段名   # 过滤字段中的重复记录
```

我们先找出部门里前 3 高的薪水，意思是不超过三个值比这些值大

```sql
select e1.Salary from Employee e1
    where 3 > 
    (
       select count(distinct(s2.Salary)) from Employee e2 where e2.Salary > e1.Salary and e1.DepartmentId = e2.DepartmentId
    )

```

举个栗子：
```
当 e1 = e2 = [4,5,6,7,8]

e1.Salary = 4，e2.Salary 可以取值 [5,6,7,8]，count(DISTINCT e2.Salary) = 4

e1.Salary = 5，e2.Salary 可以取值 [6,7,8]，count(DISTINCT e2.Salary) = 3

e1.Salary = 6，e2.Salary 可以取值 [7,8]，count(DISTINCT e2.Salary) = 2

e1.Salary = 7，e2.Salary 可以取值 [8]，count(DISTINCT e2.Salary) = 1

e1.Salary = 8，e2.Salary 可以取值 []，count(DISTINCT e2.Salary) = 0

最后 3 > count(DISTINCT e2.Salary)，所以 e1.Salary 可取值为 [6,7,8]，即集合前 3 高的薪水
```

再把表 Department 和表 Employee 连接，获得各个部门工资前三高的员工。

```sql
SELECT
	Department.NAME AS Department,
	e1.NAME AS Employee,
	e1.Salary AS Salary 
FROM
	Employee AS e1,Department 
WHERE
	e1.DepartmentId = Department.Id 
	AND 3 > (SELECT  count( DISTINCT e2.Salary ) 
			 FROM	Employee AS e2 
			 WHERE	e1.Salary < e2.Salary 	AND e1.DepartmentId = e2.DepartmentId 	) 
ORDER BY Department.NAME,Salary DESC;
```

