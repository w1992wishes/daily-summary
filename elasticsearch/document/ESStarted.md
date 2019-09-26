# 【ES】ElasticSearch 入门使用

[TOC]

内容来自 《elasticsearch 权威指南》，然后根据 es 版本做了一些简单错误修复。

## 一、CURL 命令

CURL 是利用 URL 语法在命令行方式下工作的开源文件传输工具，使用 CURL 可以简单实现常见的 get/post 请求：

- -X 指定 http 请求的方法：HEAD GET POST PUT DELETE
- -d 指定要传输的数据

比如：**curl -XHEAD 'https://www.baidu.com'**

## 二、创建索引

Megacorp 人力资源部门出于某种目的需要让创建一个员工目录，这个目录用于促进人文关怀和用于实时协同工作，所以它有以下不同的需求：

- 数据能够包含多个值的标签、数字和纯文本
- 检索任何员工的所有信息
- 支持结构化搜索，例如查找30岁以上的员工
- 支持简单的全文搜索和更复杂的短语(phrase)搜索
- 高亮搜索结果中的关键字
- 能够利用图表管理分析这些数据

POST 和 PUT 都可以，PUT 是幂等方法，POST 不是。所以 PUT 用于更新、POST 用于新增比较合适。

```shell
curl -XPOST localhost:9200/megacorp/employee/1 -d '
{
    "first_name": "John",
    "last_name": "Smith",
    "age": 25,
    "about": "I love to go rock climbing",
    "interests": [
        "sports",
        "music"
    ]
}'
```

ES6.0 之后直接运行会报错，需加上  -H "Content-Type: application/json":

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/megacorp/employee/1 -d '
{
    "first_name": "John",
    "last_name": "Smith",
    "age": 25,
    "about": "I love to go rock climbing",
    "interests": [
        "sports",
        "music"
    ]
}'
```

可以看到 path:/megacorp/employee/1 包含三部分信息：

- megacorp 索引名
- employee 类型名
- 1 这个员工的ID
- 请求实体（JSON文档），包含了这个员工的所有信息

创建索引注意事项：

- 索引库名称必须要全部小写，不能以下划线开头，也不能包含逗号
- 如果没有明确指定索引数据的 ID，那么 es 会自动生成一个随机的 ID

## 二、检索文档

### 3.1、简单搜索

**1）搜索单个员工**

只要执行 HTTP GET 请求并指出文档的“地址”——索引、类型和 ID 既可：

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/1
```

响应的内容中包含一些文档的元信息，原始 JSON 文档包含在 _source 字段中：

```json
{
    "_index": "megacorp",
    "_type": "employee",
    "_id": "1",
    "_version": 4,
    "found": true,
    "_source": {
        "first_name": "John",
        "last_name": "Smith",
        "age": 25,
        "about": "I love to go rock climbing",
        "interests": [
            "sports",
            "music"
        ]
    }
}
```

**2）搜索全部员工**

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search
```

依然使用 megacorp 索引和 employee 类型，但是在结尾使用关键字 _search 来取代原来的文档 ID，响应内容的 hits 数组中包含了所有文档。默认情况下搜索会返回前10个结果：

```json
{
    "took": 7,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 3,
        "max_score": 1,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 1,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "3",
                "_score": 1,
                "_source": {
                    "first_name": "Douglas",
                    "last_name": "Fir",
                    "age": 35,
                    "about": "I like to build cabinets",
                    "interests": [
                        "forestry"
                    ]
                }
            }
        ]
    }
}
```

**3）搜索姓氏中包含“Smith”的员工**

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search?q=last_name:Smith
```

请求中依旧使用 _search 关键字，然后将查询语句传递给参数 q=，这样就可以得到所有姓氏为 Smith 的结果：

```json
{
    "took": 125,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 0.2876821,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 0.2876821,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 0.2876821,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            }
        ]
    }
}
```

### 3.2、使用 DSL 语句搜索

Elasticsearch 提供丰富且灵活的查询语言叫做 DSL 查询(Query DSL)，它允许构建
更加复杂、强大的查询。

DSL(Domain Specific Language 特定领域语言)以 JSON 请求体的形式出现，可以这样表示之前关于“Smith”的查询:

```json
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d'
{
    "query": {
        "match": {
            "last_name": "Smith"
        }
    }
}'
```

### 3.3、复杂搜索

依旧想要找到姓氏为“Smith”的员工，但是只想得到年龄大于30岁的员工， 使用bool / must / filter 查询：

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "query" : {
        "bool" : {
            "filter" : {
                "range" : {
                    "age" : { "gt" : 30 }
                }
            },
            "must" : {
                "match" : {
                    "last_name" : "smith"
                }
            }
        }
    }
}'
```

### 3.4、全文搜索

到目前为止搜索都很简单：搜索特定的名字，通过年龄筛选。下面尝试一种更高级的搜索，全文搜索——一种传统数据库很难实现的功能。

搜索所有喜欢“rock climbing”的员工：

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "query": {
        "match": {
            "about": "rock climbing"
        }
    }
}'
```

可以得到两个匹配文档：

```json
{
    "took": 15,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 0.5753642,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 0.5753642,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 0.2876821,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            }
        ]
    }
}
```

score 表示结果相关性评分，默认情况下，Elasticsearch 结果相关性评分来对结果集进行排序，所谓的「结果相关性评分」就是文档与查询条件的匹配程度。

### 3.5、短语搜索

想要确切的匹配若干个单词或者短语(phrases)。例如想要查询同时包含"rock"和"climbing"（并且是相邻的）的员工记录。

要做到这个，要将 match 查询变更为 match_phrase 查询即可:

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "query": {
        "match_phrase": {
            "about": "rock climbing"
        }
    }
}'
```

该查询返回John Smith的文档：

```json
{
    "took": 21,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 0.5753642,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 0.5753642,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            }
        ]
    }
}
```

### 3.6、高亮搜索

从每个搜索结果中高亮(highlight)匹配到的关键字，们在语句上增加 highlight 参数：

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "query": {
        "match_phrase": {
            "about": "rock climbing"
        }
    },
    "highlight": {
        "fields": {
            "about": {}
        }
    }
}'
```

运行这个语句时，会命中与之前相同的结果，但是在返回结果中会有一个新的部分叫做 highlight，这里包含了来自 about 字段中的文本，并且用 `<em></em>` 来标识匹配到的单词。

```json
{
    "took": 120,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 0.5753642,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 0.5753642,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                },
                "highlight": {
                    "about": [
                        "I love to go <em>rock</em> <em>climbing</em>"
                    ]
                }
            }
        ]
    }
}
```

## 三、分析文档

Elasticsearch 有一个功能叫做聚合(aggregations)，它允许在数据上生成复杂的分析统计。它很像 SQL 中的 GROUP BY，但是功能更强大。

**1）找到所有职员中最大的共同点（兴趣爱好）是什么：**

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "aggs": {
        "all_interests": {
            "terms": {
                "field": "interests"
            }
        }
    }
}'
```

5.x 后对排序，聚合这些操作用单独的数据结构(fielddata)缓存到内存里了，聚合前，需要将相应的字段开启聚合需要单独开启：

```shell
curl -H "Content-Type: application/json" -XPUT localhost:9200/megacorp/_mapping/employee/ -d '
{
  "properties": {
    "interests": { 
      "type":  "text",
      "fielddata": true
    }
  }
}'
```

查询结果：

```json
{
    "took": 43,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 3,
        "max_score": 1,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 1,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "3",
                "_score": 1,
                "_source": {
                    "first_name": "Douglas",
                    "last_name": "Fir",
                    "age": 35,
                    "about": "I like to build cabinets",
                    "interests": [
                        "forestry"
                    ]
                }
            }
        ]
    },
    "aggregations": {
        "all_interests": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
                {
                    "key": "music",
                    "doc_count": 2
                },
                {
                    "key": "forestry",
                    "doc_count": 1
                },
                {
                    "key": "sports",
                    "doc_count": 1
                }
            ]
        }
    }
}
```

**2）如果想知道所有姓"Smith"的人最大的共同点（兴趣爱好），只需要增加合适的语句既可：**

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "query": {
        "match": {
            "last_name": "smith"
        }
    },
    "aggs": {
        "all_interests": {
            "terms": {
                "field": "interests"
            }
        }
    }
}'
```

all_interests 聚合已经变成只包含和查询语句相匹配的文档了：

```json
{
    "took": 11,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 0.2876821,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 0.2876821,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 0.2876821,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            }
        ]
    },
    "aggregations": {
        "all_interests": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
                {
                    "key": "music",
                    "doc_count": 2
                },
                {
                    "key": "sports",
                    "doc_count": 1
                }
            ]
        }
    }
}
```

3）聚合也允许分级汇总。例如，统计每种兴趣下职员的平均年龄：

```shell
curl -H "Content-Type: application/json" -XGET localhost:9200/megacorp/employee/_search -d '
{
    "aggs": {
        "all_interests": {
            "terms": {
                "field": "interests"
            },
            "aggs": {
                "avg_age": {
                    "avg": {
                        "field": "age"
                    }
                }
            }
        }
    }
}'
```

返回的聚合结果有些复杂：

```json
{
    "took": 29,
    "timed_out": false,
    "_shards": {
        "total": 5,
        "successful": 5,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 3,
        "max_score": 1,
        "hits": [
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "first_name": "Jane",
                    "last_name": "Smith",
                    "age": 32,
                    "about": "I ike to collect rock albums",
                    "interests": [
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "1",
                "_score": 1,
                "_source": {
                    "first_name": "John",
                    "last_name": "Smith",
                    "age": 25,
                    "about": "I love to go rock climbing",
                    "interests": [
                        "sports",
                        "music"
                    ]
                }
            },
            {
                "_index": "megacorp",
                "_type": "employee",
                "_id": "3",
                "_score": 1,
                "_source": {
                    "first_name": "Douglas",
                    "last_name": "Fir",
                    "age": 35,
                    "about": "I like to build cabinets",
                    "interests": [
                        "forestry"
                    ]
                }
            }
        ]
    },
    "aggregations": {
        "all_interests": {
            "doc_count_error_upper_bound": 0,
            "sum_other_doc_count": 0,
            "buckets": [
                {
                    "key": "music",
                    "doc_count": 2,
                    "avg_age": {
                        "value": 28.5
                    }
                },
                {
                    "key": "forestry",
                    "doc_count": 1,
                    "avg_age": {
                        "value": 35
                    }
                },
                {
                    "key": "sports",
                    "doc_count": 1,
                    "avg_age": {
                        "value": 25
                    }
                }
            ]
        }
    }
}
```

