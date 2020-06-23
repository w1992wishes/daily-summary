## 模块代码实现

### HotItems

将实现一个“实时热门商品”的需求，

可以将“实时热门商品”翻译成程序员更好理解的需求：每隔 5 分钟输出最近一小时内点击量最多的前N个商品。

将这个需求进行分解大概要做这么几件事情：

* 抽取出业务时间戳，告诉 Flink 框架基于业务时间做窗口
* 过滤出点击行为数据
* 按一小时的窗口大小，每5分钟统计一次，做滑动窗口聚合（Sliding Window）
* 按每个窗口聚合，输出每个窗口中点击量前 N 名的商品

### NetworkTrafficAnalysis

实现的模块是 “实时流量统计”。

对于一个电商平台而言，用户登录的入口流量、不同页面的访问流量都是值得分析的重要数据，而这些数据，可以简单地从 web 服务器的日志中提取出来。

在这里实现最基本的“页面浏览数”的统计，也就是读取服务器日志中的每一行 log，统计在一段时间内用户访问 url 的次数。

具体做法为：

每隔 5 秒，输出最近 10 分钟内访问量最多的前 N 个 URL。可以看出，这个需求与之前“实时热门商品统计”非常类似，所以完全可以借鉴此前的代码。

### LoginFailDetect

恶意登录监控。

对于网站而言，用户登录并不是频繁的业务操作。如果一个用户短时间内频繁登录失败，就有可能是出现了程序的恶意攻击，比如密码暴力破解。

因此我们考虑，应该对用户的登录失败动作进行统计，具体来说，如果同一用户（可以是不同IP）在2秒之内连续两次登录失败，就认为存在恶意登录的风险，输出相关的信息进行报警提示。

这是电商网站、也是几乎所有网站风控的基本一环。

由于同样引入了时间，我们可以想到，最简单的方法其实与之前的热门统计类似，只需要按照用户ID分流，然后遇到登录失败的事件时将其保存在ListState中，然后设置一个定时器，2秒后触发。

定时器触发时检查状态中的登录失败事件个数，如果大于等于2，那么就输出报警信息。

### OrderTimeoutDetect

订单支付实时监控.

在电商平台中，最终创造收入和利润的是用户下单购买的环节；更具体一点，是用户真正完成支付动作的时候。用户下单的行为可以表明用户对商品的需求，但在现实中，并不是每次下单都会被用户立刻支付。

当拖延一段时间后，用户支付的意愿会降低。所以为了让用户更有紧迫感从而提高支付转化率，同时也为了防范订单支付环节的安全风险，电商网站往往会对订单状态进行监控，设置一个失效时间（比如15分钟），如果下单后一段时间仍未支付，订单就会被取消。
