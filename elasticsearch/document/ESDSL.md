# 【ES】ElasticSearch 结构化查询和过滤

[TOC]

## 一、DSL

在 ElasticSearch 中，提供了功能十分丰富、多种表现形式的查询语言—— DSL 查询。

Query DSL 又叫结构化查询，使用 JSON 格式的请求体 与 ElasticSearch 交互，使查询语句更灵活、更精确、更易读且易调试。

使用结构化查询，你需要传递 `query` 参数：

```Javascript
GET /_search
{
    "query": YOUR_QUERY_HERE
}
```

主要包含两种类型的查询语句：**叶子查询语句**和**复合查询语句**。

### 1.1、叶子查询语句

这种查询可以单独使用，**针对指定的字段查询指定的值**，例如：match, term, range 等。

一个叶子查询语句一般使用这种结构：

```Javascript
{
    QUERY_NAME: {
        ARGUMENT: VALUE,
        ARGUMENT: VALUE,...
    }
}
```

或指向一个指定的字段：

```Javascript
{
    QUERY_NAME: {
        FIELD_NAME: {
            ARGUMENT: VALUE,
            ARGUMENT: VALUE,...
        }
    }
}
```

例如，可以使用 `match` 查询子句用来找寻在 `tweet` 字段中找寻包含 `elasticsearch` 的成员：

```Javascript
GET /_search
{
    "query": {
        "match": {
            "tweet": "elasticsearch"
        }
    }
}
```

### 1.2、复合查询语句

这种查询可以合并其他的叶子查询或复合查询，从而实现非常复杂的查询逻辑。

例如，`bool` 子句允许合并其他的合法子句，`must`，`must_not` 或者 `should`：

```Javascript
{
    "bool": {
        "must":     { "match": { "tweet": "elasticsearch" }},
        "must_not": { "match": { "name":  "mary" }},
        "should":   { "match": { "tweet": "full text" }}
    }
}
```

## 二、Query DSL 和 Filter DSL

Elasticsearch 使用的查询语言（DSL） 拥有一套查询组件，这些组件可以以无限组合的方式进行搭配。这套组件可以在以下两种情况下使用：**查询情况 `query context`**和**过滤情况 `filtering context`** ，也即**结构化查询 `Query DSL`** 和**结构化过滤 `Filter DSL`**。

查询与过滤语句非常相似，但是它们由于使用目的不同而稍有差异。

### 2.1、Query DSL

在上下文查询语境中，查询语句会询问文档与查询语句的匹配程度，它会判断文档是否匹配并计算相关性评分（_score）的值。

例如：

- 查找与 `full text search` 这个词语最佳匹配的文档
- 查找包含单词 `run`，但是也包含`runs`, `running`, `jog` 或 `sprint`的文档
- 同时包含着 `quick`, `brown` 和`fox`--- 单词间离得越近，该文档的相关性越高
- 标识着 `lucene`, `search` 或 `java`--- 标识词越多，该文档的相关性越高

一条查询语句会计算每个文档与查询语句的相关性，然后给出一个相关性评分 `_score`，并且按照相关性对匹配到的文档进行排序。 

### 2.2、Filter DSL

在上下文过滤语境中，查询语句主要解决文档是否匹配的问题，而不会在意匹配程度（相关性评分）。

例如：

- `created` 的日期范围是否在 `2013` 到 `2014` ?
- `status` 字段中是否包含单词 "published" ?
- `lat_lon` 字段中的地理位置与目标点相距是否不超过10km ?

### 2.3、比较

相关度：

- `filter` —— 只根据搜索条件过滤出符合的文档，将这些文档的评分固定为1，忽略 TF/IDF 信息，不计算相关度分数；
- `query` —— 先查询符合搜索条件的文档， 然后计算每个文档对于搜索条件的相关度分数，再根据评分倒序排序。

性能：

- `filter` 性能更好，无排序 —— 不计算相关度分数，不用根据相关度分数进行排序，同时 ES 内部还会缓存(cache)比较常用的 filter 的数据 (使用bitset <0或1> 来记录包含与否)；
- `query`性能较差， 有排序 —— 要计算相关度分数， 要根据相关度分数进行排序， 并且没有cache功能。

**原则上来说，使用查询语句做全文本搜索或其他需要进行相关性评分的时候，剩下的全部用过滤语句。**

在进行搜索时，常常会在查询语句中，结合查询和过滤来达到查询目的：

```json
{
    "bool": {
        "must":     { "match": { "title": "how to make millions" }},
        "must_not": { "match": { "tag":   "spam" }},
        "should": [
            { "match": { "tag": "starred" }}
        ],
        "filter": {
          "range": { "date": { "gte": "2014-01-01" }} 
        }
    }
}
```

## 三、重要的查询过滤语句

### 3.1、match

`match`查询是一个标准查询，不管全文本查询还是精确查询基本上都要用到它。

如果使用 `match` 查询一个全文本字段，它会在真正查询之前用分析器先分析查询字符：

```Javascript
{
    "match": {
        "tweet": "About Search"
    }
}
```

如果用`match`下指定了一个确切值，在遇到数字，日期，布尔值或者`not_analyzed` 的字符串时，它将搜索给定的值：

```Javascript
{ "match": { "age":    26           }}
{ "match": { "date":   "2014-09-01" }}
{ "match": { "public": true         }}
{ "match": { "tag":    "full_text"  }}
```

**提示**： 做精确匹配搜索时，最好用过滤语句，因为过滤语句可以缓存数据。

### 3.2、multi_match

`multi_match`查询允许做`match`查询的基础上同时搜索多个字段：

```Javascript
{
    "multi_match": {
        "query":    "full text search",
        "fields":   [ "title", "body" ]
    }
}

```

### 3.3、match_phrase

短语查询，精确匹配。查询`a red`会匹配包含`a red`短语的，而不会进行分词查询，也不会查询出包含`a 其他词 red`这样的文档。

```javascript
{
    "query": {
        "match_phrase": {
            "ad": "a red"
        }
    }
}

```

### 3.4、match_all

使用`match_all` 可以查询到所有文档，是没有查询条件下的默认语句：

```Javascript
{
    "match_all": {}
}

```

此查询常用于合并过滤条件。 比如说需要检索所有的邮箱，所有的文档相关性都是相同的，所以得到的`_score`为1。

### 3.5、term

`term`主要用于精确匹配哪些值，比如数字，日期，布尔值或 `not_analyzed`的字符串(即不进行分词器分析，文档中必须包含整个搜索的词汇)：

```Javascript
{ "term": { "age":    26           }}
{ "term": { "date":   "2014-09-01" }}
{ "term": { "public": true         }}
{ "term": { "tag":    "full_text"  }}

```

### 3.6、terms

`terms` 跟 `term` 有点类似，但 `terms` 允许指定多个匹配条件。 如果某个字段指定了多个值，那么文档需要一起去做匹配，类似于 MySQL 的 in 条件：

```Javascript
{
    "terms": {
        "tag": [ "search", "full_text", "nosql" ]
        }
}

```

### 3.7、range

`range`允许按照指定范围查找一批数据：

```Javascript
{
    "range": {
        "age": {
            "gte":  20,
            "lt":   30
        }
    }
}

```

范围操作符包含：

- `gt` :: 大于
- `gte`:: 大于等于
- `lt` :: 小于
- `lte`:: 小于等于

### 3.8、exists 

用于查找那些指定字段中有值或无值的文档。

指定`title`字段有值：

```javascript
{
    "exists":   {
        "field":    "title"
    }
}

```

指定`title`字段无值：

```Javascript
{
    "query": {
        "bool": {
            "must_not": {
                "exists": {
                    "field": "group"
                }
            }
        }
    }
}

```

注：missing 查询无值已经被取消。

### 3.9、bool

`bool` 可以用来合并多个条件查询结果的布尔逻辑，它包含一下操作符：

- `must` :: 多个查询条件的完全匹配，相当于 `and`
- `should` :: 至少有一个查询条件匹配，相当于 `or`
- `must_not` :: 多个查询条件的相反匹配，相当于 `not`，忽略相关性评分
- `filter`:: 必须匹配，忽略相关性评分

```javascript
POST /_search
{
    "query": {
        "bool" : {
            "must" : {
              "term" : { "last_name" : "smith" }
            },
            "filter": {
              "term" : { "info.interests" : "musics" }
            },
            "must_not" : {
              "range" : {
                "info.age" : { "gte" : 10, "lte" : 25 }
              }
            },
            "should" : [
              { "term" : { "full_name" : "john" } },
              { "term" : { "full_name" : "smith" } }
            ]
        }
    }
}

```

**提示**： 如果`bool` 查询下没有`must`子句，那至少应该有一个`should`子句。但是 如果有`must`子句，那么没有`should`子句也可以进行查询。

## 四、验证查询

查询语句可以变得非常复杂，特别是与不同的分析器和字段映射相结合后，就会有些难度。

`validate` API 可以验证一条查询语句是否合法。

```Javascript
GET /gb/tweet/_validate/query
{
   "query": {
      "tweet" : {
         "match" : "really powerful"
      }
   }
}

```

请求的返回值说明这条语句是非法的：

```Javascript
{
  "valid" :         false,
  "_shards" : {
    "total" :       1,
    "successful" :  1,
    "failed" :      0
  }
}

```

想知道语句非法的具体错误信息，需要加上 `explain` 参数：

```Javascript
GET /gb/tweet/_validate/query?explain 
{
   "query": {
      "tweet" : {
         "match" : "really powerful"
      }
   }
}

```

`explain` 参数可以提供语句错误的更多详情，很显然，这里把 query 语句的 `match` 与字段名位置弄反了。