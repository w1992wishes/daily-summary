# 【ES】ElasticSearch 映射（mapping）

[TOC]

## 一、定义

映射(mapping)即是模式定义(schema definition)。一个映射定义了字段类型，每个字段的数据类型，以及字段被 Elasticsearch 处理的方式。映射还用于设置关联到类型上的元数据。

可以说，映射就是对索引库中索引的字段名称及其数据类型进行定义，类似于 mysql 中的表结构信息。

映射可以分为动态映射和显式映射。

### 1.1、动态映射 （dynamic mapping）

在关系数据库中，需要事先创建数据库，然后在该数据库实例下创建数据表，然后才能在该数据表中插入数据。而 ElasticSearch中 不需要事先定义映射（Mapping），文档写入 ElasticSearch 时，会根据文档字段自动识别类型，这种机制称之为动态映射。

```json
# 动态映射：如果 mapping 没有创建，elasticsearch 将会根据写入的数据的 key 自行创建相应的 mapping，并写入数据
curl -H "Content-Type: application/json" -XPUT localhost:9200/sports/basketball/123 -d '
{
    "date": "2018-05-20",
    "title": "Cavaliers",
    "content": "They are real Cavaliers tonigiht",
    "author_id": 520
}'

# 可以使用 _mapping 查询索引的 mapping
curl -XGET localhost:9200/sports/basketball/_mapping?pretty
# 返回结果
{
  "sports" : {
    "mappings" : {
      "basketball" : {
        "properties" : {
          "author_id" : {
            "type" : "long"
          },
          "content" : {
            "type" : "text",
            "fields" : {
              "keyword" : {
                "type" : "keyword",
                "ignore_above" : 256
              }
            }
          },
          "date" : {
            "type" : "date"
          },
          "title" : {
            "type" : "text",
            "fields" : {
              "keyword" : {
                "type" : "keyword",
                "ignore_above" : 256
              }
            }
          }
        }
      }
    }
  }
}
```

### 1.2、显式映射（explicit mappings）

在 ElasticSearch 中也可以事先定义好映射，包含文档的各个字段及其类型等，这种方式称之为显式映射。

```json
# 静态映射，手动设置
curl -H "Content-Type: application/json" -XPUT localhost:9200/books?pretty -d '
{
    "settings": {
        "number_of_shards": 3,
        "number_of_replicas": 0
    },
    "mappings": {
        "novel": {
            "properties": {
                "title": {
                    "type": "text"
                },
                "name": {
                    "type": "text",
                    "analyzer": "standard",
                    "search_analyzer": "standard"
                },
                "publish_date": {
                    "type": "date"
                },
                "price": {
                    "type": "double"
                },
                "number": {
                    "type": "integer"
                }
            }
        }
    }
}'
```

ES 通过猜测来确定字段类型，动态映射将会很有用，但**如果**需要对某些字段添加特殊属性（如：定义使用其它分词器、是否分词、是否存储等），**就必须显式映射**。

## 二、数据类型及支持属性

### 2.1、核心类型（Core datatype）

**字符串**：`string`，string 类型包含 *text* 和 *keyword*。ELasticsearch 5.X 之后的字段类型不再支持 string，由 text 或 keyword 取代。 

- text：当一个字段是要被全文搜索的，比如 Email 内容、产品描述，应该使用 text 类型。设置 text 类型以后，字段内容会被分析，在生成倒排索引以前，字符串会被分析器分成一个一个词项。text 类型的字段不用于排序，很少用于聚合（termsAggregation 除外）。
- keyword：该类型不需要进行分词，可以被用来检索过滤、排序和聚合，keyword 类型的字段只能通过精确值搜索到（不可用 text 分词后的模糊检索）。

**整数**：`byte`, `short`, `integer`, `long`

**浮点数**：`float`, `double`

**布尔**：`boolean`

**日期**：`date`，JSON 本身并没有日期数据类型，在 ES 中的日期类型可以是：

- 类似 `2015-01-01` or `2015/01/01 12:10:30` 的字符串
- long 类型的毫秒级别的时间戳
- int 类型的秒级别的时间戳
- 日期类型默认会被转换为 UTC 并且转换为毫秒级别的时间戳的 long 类型存储。日期类型如果不指定 format ，将会以默认格式表示。

**二进制型**：`binary``，二进制类型以 Base64 编码方式接收一个二进制值，二进制类型字段默认不存储，也不可搜索。

**除 *text* 类型以外，其他类型必须要进行精确查询，因为除 *text* 外其他类型不会进行分词。**

```sh
# 新增
curl -H "Content-Type: application/json" -XPOST localhost:9200/books/novel/1 -d '
{
    "name": "being alive",
    "number": "1000",
    "price": 17.99,
    "publish_date": "2018-05-20",
    "title": "being Live"
}'
# 新增
curl -H "Content-Type: application/json" -XPOST localhost:9200/books/novel/2 -d '
{
    "name": "baby",
    "number": "100",
    "price": 27.99,
    "publish_date": "2018-05-16",
    "title": "a girl is a baby"
}'


# date 类型不会进行分词，必须精确查询，此时查询不出数据
curl -XGET localhost:9200/books/novel/_search?q=publish_date:2018
# 返回结果
{
    "took": 1,
    "timed_out": false,
    "_shards": {
        "total": 3,
        "successful": 3,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 0,
        "max_score": null,
        "hits": []
    }
}

# date 类型不会进行分词，必须精确查询，此时可以查询出数据
curl -XGET localhost:9200/books/novel/_search?q=publish_date:2018-05-20
# 返回结果
{
    "took": 2,
    "timed_out": false,
    "_shards": {
        "total": 3,
        "successful": 3,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 1,
        "hits": [
            {
                "_index": "books",
                "_type": "novel",
                "_id": "1",
                "_score": 1,
                "_source": {
                    "name": "being alive",
                    "number": "1000",
                    "price": 17.99,
                    "publish_date": "2018-05-20",
                    "title": "being Live"
                }
            }
        ]
    }
}

# text 会进行分词，此时可以查出数据
curl -XGET localhost:9200/books/novel/_search?q=title:girl
# 返回结果
{
    "took": 3,
    "timed_out": false,
    "_shards": {
        "total": 3,
        "successful": 3,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 0.2876821,
        "hits": [
            {
                "_index": "books",
                "_type": "novel",
                "_id": "2",
                "_score": 0.2876821,
                "_source": {
                    "name": "baby",
                    "number": "100",
                    "price": 27.99,
                    "publish_date": "2018-05-16",
                    "title": "a girl is a baby"
                }
            }
        ]
    }
}
```

### 2.2、复合类型（Complex datatypes）

#### 2.2.1、对象

JSON 文档是有层次结构的，一个文档可能包含其他文档，如果一个文档包含其他文档，那么该文档值是对象类型，其数据类型是对象，ES 默认把文档的属性 type设置为 object，即 "type":"object"。

在 ES 内部，"对象" 被索引为一个扁平的键值对。

```sh
curl -H "Content-Type: application/json" -XPOST localhost:9200/archive/person/1?pretty -d '
{
    "region": "US",
    "manager": {
        "age": 30,
        "name": {
            "first": "John",
            "last": "Smith"
        }
    }
}
```

默认情况下，上述文档类型被索引为以点号命名的数据结构，把层次结构展开之后，数据结构是由扁平的key/value对构成：

```json
{
    "region": "US",
    "manager.age": 30,
    "manager.name.first": "John",
    "manager.name.last": "Smith"
}
```

#### 2.2.2、数组

在 ElasticSearch 中，没有专门的数组（Array）数据类型，是开箱即用的（out of box），不需要进行任何配置，就可以直接使用。

在默认情况下，任意一个字段都可以包含 0 或多个值，这意味着每个字段默认都是数组类型，只不过，数组类型的各个元素值的数据类型必须相同：

- 字符串数组: [ "one", "two" ]
- 整型数组: [ 1, 2 ]
- 数组数组: [ 1, [ 2, 3 ]] which is the equivalent of [ 1, 2, 3 ]
- 对象数组: [ { "name": "Mary", "age": 12 }, { "name": "John", "age": 10 }] 

注意：

- 动态添加数据时，数组的第一个值的类型决定整个数组的类型
- 混合数组类型是不支持的，比如：[1,”abc”]
- 数组可以包含 null 值，空数组 [ ] 会被当做 missing field 对待

**对象数组，在 ES 内部将会被转换为 "多值" 的扁平数据类型：**

```sh
curl -H "Content-Type: application/json" -XPOST localhost:9200/lib/club/1?pretty -d '
{
    "group": "fans",
    "user": [
        {
            "first": "John",
            "last": "Smith"
        },
        {
            "first": "Alice",
            "last": "White"
        }
    ]
}'
# 在 es 内部将转为
{
    "group": "fans",
    "user.first": [
        "alice",
        "john"
    ],
    "user.last": [
        "smith",
        "white"
    ]
}
```

**这将导致每个数组元素（对象）内部的字段关联性丢失**:

```sh
curl -H "Content-Type: application/json" -XGET localhost:9200/lib/club/_search?pretty -d '
{
    "query": {
        "bool": {
            "must": [
                {
                    "match": {
                        "user.first": "John"
                    }
                },
                {
                    "match": {
                        "user.last": "White"
                    }
                }
            ]
        }
    }
}'
```

字段 user.first 和 user.last 被展开成数组字段，但是，这样展开之后，单个文档内部的字段之间的关联就会丢失，在该例中，展开的文档数据丢失 first 和 last 字段之间的关联，比如，A`lice` 和 `white` 的关联就丢失了。

```json
# 文档被命中，显然，John Smith 、Alice White 两组字段它们内在的关联性都丢失了
{
  "took" : 3,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : 1,
    "max_score" : 0.5753642,
    "hits" : [
      {
        "_index" : "lib",
        "_type" : "club",
        "_id" : "1",
        "_score" : 0.5753642,
        "_source" : {
          "group" : "fans",
          "user" : [
            {
              "first" : "John",
              "last" : "Smith"
            },
            {
              "first" : "Alice",
              "last" : "White"
            }
          ]
        }
      }
    ]
  }
}
```

#### 2.2.3、嵌套数据

嵌套数据类型是对象数据类型的特殊版本，它允许对象数组中的各个对象被索引，数组中的各个对象之间保持独立，能够对每一个文档进行单独查询。嵌套数据类型保留文档的内部之间的关联。

ElasticSearch 引擎内部使用不同的方式处理嵌套数据类型和对象数组的方式，对于嵌套数据类型，ElasticSearch 把数组中的每一个嵌套文档（Nested Document）索引为单个文档，这些文档是隐藏（Hidden）的，文档之间是相互独立的，但是，保留文档的内部字段之间的关联，使用嵌套查询（Nested Query）能够独立于其他文档而去查询单个文档。

在创建嵌套数据类型的字段时，需要设置字段的type属性为nested。

```sh
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index?pretty -d '
{
    "mappings": {
        "my_type": {
            "properties": {
                "group": {
                    "type": "keyword"
                },
                "user": {
                    "type": "nested",
                    "properties": {
                        "first": {
                            "type": "keyword"
                        },
                        "last": {
                            "type": "keyword"
                        }
                    }
                }
            }
        }
    }
}'

```

为嵌套字段赋予多个值，ElasticSearch 自动把字段值转换为数组类型：

```sh
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index/my_type/1 -d '
{
    "group": "fans",
    "user": [
        {
            "first": "John",
            "last": "Smith"
        },
        {
            "first": "Alice",
            "last": "White"
        }
    ]
}'

```

在 ElasticSearch 内部，嵌套的文档（Nested Documents）被索引为很多独立的隐藏文档（separate documents），这些隐藏文档只能通过嵌套查询（Nested Query）访问。每一个嵌套的文档都是嵌套字段（文档数组）的一个元素。嵌套文档的内部字段之间的关联被 ElasticSearch 引擎保留，而嵌套文档之间是相互独立的。

嵌套查询用于查询嵌套对象，执行嵌套查询执行的条件是：嵌套对象被索引为单个文档，查询作用在根文档（Root Parent）上。嵌套查询由关键字“nested”指定：

```json
{
    "nested": {
        "path": "obj1",
        "query": {...}
    }
}

```

- path 参数：指定嵌套字段的文档路径，根路径是顶层的文档，通过点号“.”来指定嵌套文档的路径；
- query 参数：在匹配路径（参数 path）的嵌套文档上执行查询，query参数指定对嵌套文档执行的查询条件。

```json
curl -H "Content-Type: application/json" -XGET localhost:9200/my_index/my_type/_search?pretty -d '
{
    "query": {
        "nested": {
            "path": "user",
            "query": {
                "bool": {
                    "must": [
                        {
                            "match": {
                                "user.first": "John"
                            }
                        },
                        {
                            "match": {
                                "user.last": "White"
                            }
                        }
                    ]
                }
            }
        }
    }
}'

```

在该例中，ElasticSearch 引起保留 Alice 和 White 之间的关联，而 John 和 White 之间是没有任何关联的：

```json
{
  "took" : 3,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : 0,
    "max_score" : null,
    "hits" : [ ]
  }
}

```

### 2.3、地理位置类型（Geo datatypes）

地理位置信息类型用于存储地理位置信息的经纬度，地理坐标点不能被动态映射（dynamic mapping）自动检测，而是需要显式声明对应字段类型为 `geo_point` 。

```sh
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index1?pretty -d '
{
    "mappings": {
        "my_type1": {
            "properties": {
                "location": {
                    "type": "geo_point"
                }
            }
        }
    }
}'

```

`location` 被声明为 `geo_point` 后，就可以索引包含了经纬度信息的文档了。经纬度信息的形式可以是字符串，数组或者对象。

```sh
# 明确以 lat 和 lon 作为属性的对象；
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index1/my_type1/1?pretty -d '
{
    "text": "Geo-point as an object",
    "location": {
        "lat": 41.12,
        "lon": -71.34
    }
}'

# 以半角逗号分割的字符串形式 "lat,lon"
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index1/my_type1/2?pretty -d '
{
    "text": "Geo-point as a string",
    "location": "41.12,-71.34"
}'

curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index1/my_type1/3?pretty -d '
{
    "text": "Geo-point as a geohash",
    "location": "drm3btev3e86"
}'

# 数组形式表示 [lon,lat]
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_index1/my_type1/4?pretty -d '
{
    "text": "Geo-point as an array",
    "location": [
        -71.34,
        41.12
    ]
}'
```

有四种地理坐标点相关的过滤方式可以用来选中或者排除文档：

- `geo_bounding_box`::

  找出落在指定矩形框中的坐标点

- `geo_distance`::

  找出与指定位置在给定距离内的点

- `geo_distance_range`::

  找出与指定点距离在给定最小距离和最大距离之间的点

- `geo_polygon`::

  找出落在多边形中的点。*这个过滤器使用代价很大*。

```sh
curl -H "Content-Type: application/json" -XGET localhost:9200/my_index1/my_type1/_search?pretty -d '
{
    "query": {
        "geo_bounding_box": {
            "location": {
                "top_left": {
                    "lat": 42,
                    "lon": -72
                },
                "bottom_right": {
                    "lat": 40,
                    "lon": -70
                }
            }
        }
    }
}'
```

### 2.4、特定类型（Specialised datatypes）

**IPv4 类型（IPv4 datatype）**：ip 用于IPv4 地址

**Completion 类型（Completion datatype）**：completion 提供自动补全建议

**Token count 类型（Token count datatype）**：token_count 用于统计做子标记的字段的index数目，该值会一直增加，不会因为过滤条件而减少

**mapper-murmur3 类型**：通过插件，可以通过 `_murmur3_` 来计算 index 的哈希值

**附加类型（Attachment datatype）**：采用 mapper-attachments 插件，可支持`_attachments_` 索引，例如 Microsoft office 格式，Open Documnet 格式， ePub，HTML 等

## 三、映射 parameters

https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-params.html#mapping-params

**analyzer**

分词器，如果修改分词器，要重建索引，因为分词的内容是放到倒排索引中的，如果不重建索引，是无法修改倒排索引中内容的，所以选择分词器就比较重要，比如中文分词使用 ik_smart 还是 ik_max_word。

**normalizer**

用于解析前的标准化配置，比如把所有的字符转化为小写。

**index**

index 选项控制是否索引字段值。 它接受 true 或 false，默认为 true。 未编制索引的字段不可查询。

**null_value**

无法索引或搜索空值 null。 当字段设置为 null（或空数组或空值数组）时，它被视为该字段没有值。

null_value 参数允许使用指定的值替换显式空值，以便可以对其进行索引和搜索。

## 四、参考资料

1.ES 权威指南
2.https://blog.csdn.net/zx711166/article/details/81667862
3.https://blog.csdn.net/ZYC88888/article/details/83059040

