## 1
有如下的用户访问数据

userId visitDate visitCount
u01 2017/1/21 5
u02 2017/1/23 6
u03 2017/1/22 8
u04 2017/1/20 3
u01 2017/1/23 6
u01 2017/2/21 8
U02 2017/1/23 6
U01 2017/2/22 4

要求使用SQL统计出每个用户的累积访问次数，如下表所示：

用户id 月份 小计 累积
u01 2017-01 11 11
u01 2017-02 12 23
u02 2017-01 12 12
u03 2017-01 8 8
u04 2017-01 3 3

```sql
select 
  userid,
  dt,
  sum,
  sum(sum) over(partition by userid order by dt rows between unbounded preceding and current row) sum2
from 
    (
    select 
      userid, 
      date_format(regexp_replace(visitdate,'/','-'),'yyyy-MM') dt,
      sum(visitcount) sum
    from sql01
    group by userid, date_format(regexp_replace(visitdate,'/','-'),'yyyy-MM')
    )
t1;
```