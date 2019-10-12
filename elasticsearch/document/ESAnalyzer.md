# 【ES】ElasticSearch analyzer 和 analyze API

[TOC]

## 一、分词

搜索引擎的核心是倒排索引，而倒排索引的基础就是分词。所谓分词可以简单理解为将一个完整的句子切割为一个个单词的过程。在 es 中单词对应英文为 `term`。 

* `读时分词`发生在用户查询时，ES 会即时地对用户输入的关键词进行分词，分词结果只存在内存中，当查询结束时，分词结果也会随即消失。
* `写时分词`发生在文档写入时，ES 会对文档进行分词后，将结果存入倒排索引，该部分最终会以文件的形式存储于磁盘上，不会因查询结束或者 ES 重启而丢失。 

ES 中处理分词的部分被称作分词器，英文是`Analyzer`，它决定了分词的规则。ES 自带了很多默认的分词器，比如 `Standard`、 `Keyword`、`Whitespace`等等，默认是 `Standard`。读时或者写时可以指定要使用的分词器。 

## 二、分析器 analyzer

### 2.1、简介

ES 会把一个文本块分析成一个个单独的词（term），为后边的倒排索引做准备。然后标准化这些词为标准形式，提高可搜索性，这些工作是分析器 analyzer 完成的。一个分析器包括： 

* character filter 字符过滤器：字符串按顺序通过每个字符过滤器，他们的任务是在分词前整理字符串，一个字符过滤器可以用来去掉 HTML 标记，或者将`&`转化成`and`。
*  tokenizer 分词器：字符串被分词器分为单个的词条，一个简单的分词器遇到空格和标点的时候，可能会将文本拆分成词条，中文分词比较复杂,可以采用机器学习算法来分词。
* token filters 表征过滤器： 最后，每个词都通过所有表征过滤（token filters），它可以修改词（例如将 Quick 转为小写），去掉词（例如停用词像 a 、 and 、 the 等等），或者增加词（例如同义词像 jump 和 leap ） 。

执行顺序是： character filter -->> tokenizer -->> token filters 。

### 2.2、中文分析器

#### 2.2.1、离线安装

**下载对应版本：**

```
https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.2.4/elasticsearch-analysis-ik-6.2.4.zip 
```

**解压到 es plugins 目录下:**

```shell
unzip elasticsearch-analysis-ik-6.2.4.zip 
```

#### 2.2.2、在线安装（推荐）

```
 ./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.2.4/elasticsearch-analysis-ik-6.2.4.zip 
```

在线或者离线安装后都需要重启 ElasticSearch 服务，观察启动日志，可以发现如下字样：

```
loaded plugin [analysis-ik] 
```

#### 2.2.3、 ik_max_word 和 ik_smart 区别 

* ik_max_word：会将文本做最细粒度的拆分，比如会将“中华人民共和国人民大会堂”拆分为“中华人民共和国、中华人民、中华、华人、人民共和国、人民、共和国、大会堂、大会、会堂等词语。  

*  ik_smart ： 会做最粗粒度的拆分，比如会将“中华人民共和国人民大会堂”拆分为中华人民共和国、人民大会堂。

* 索引时，为了提供索引的覆盖范围，通常会采用 ik_max_word 分析器，会以最细粒度分词索引，搜索时为了提高搜索准确度，会采用 ik_smart 分析器，会以粗粒度分词， mapping 设置如下 ：

```javascript
"author": {
    "type": "string",
    "analyzer": "ik",
    "search_analyzer": "ik_smart"
}
```

### 2.3、自定义分析器

 自定义完整分析器 ：

```javascript
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_test -d '
{
    "settings":{
        "analysis":{
            "char_filter":{  //具体定义字符过滤器
                "&_to_and":{
                    "type":"mapping",
                    "mappings":[
                        "& => and"
                    ]
                }
            },
            "filter":{ //具体定义 token 过滤器
                "my_stopwords":{
                    "type":"stop",
                    "stopwords":[
                        "the",
                        "a"
                    ]
                }
            },
            "analyzer":{
                "my_analyzer":{
                    "type":"custom", //自定义分词器
                    "char_filter":[
                        "html_strip",
                        "&_to_and" //自定义的字符过滤器
                    ],
                    "tokenizer":"standard",
                    "filter":[
                        "lowercase",
                        "my_stopwords" //自定义的 token 过滤器
                    ]
                }
            }
        }
    }
}'
```

在字段上使用自定义分析器 :

```javascript
curl -H "Content-Type: application/json" -XPUT localhost:9200/my_test/my_type/_mapping/ -d '
{
    "properties": {
        "title": {
            "type": "text",
            "analyzer": "my_analyzer"
        }
    }
}'
```

## 三、analyze api

### 3.1、写时分词

```javascript
curl -H "Content-Type: application/json" -XPOST localhost:9200/my_test/_analyze?pretty -d '
{
  "field": "title",
  "text": "Eating an apple & a banana a day keeps doctor away"
}'
```

其中 `my_test`为索引名，`_analyze` 为查看分词结果的 `endpoint`，请求体中 `field` 为要查看的字段名，`text`为具体值。该 api 的用于在 `my_test` 索引使用 title 字段存储一段文本时，es 会如何分词：

```json
{
  "tokens" : [
    {
      "token" : "eating",
      "start_offset" : 0,
      "end_offset" : 6,
      "type" : "<ALPHANUM>",
      "position" : 0
    },
    {
      "token" : "an",
      "start_offset" : 7,
      "end_offset" : 9,
      "type" : "<ALPHANUM>",
      "position" : 1
    },
    {
      "token" : "apple",
      "start_offset" : 10,
      "end_offset" : 15,
      "type" : "<ALPHANUM>",
      "position" : 2
    },
    {
      "token" : "and",
      "start_offset" : 16,
      "end_offset" : 17,
      "type" : "<ALPHANUM>",
      "position" : 3
    },
    {
      "token" : "banana",
      "start_offset" : 20,
      "end_offset" : 26,
      "type" : "<ALPHANUM>",
      "position" : 5
    },
    {
      "token" : "day",
      "start_offset" : 29,
      "end_offset" : 32,
      "type" : "<ALPHANUM>",
      "position" : 7
    },
    {
      "token" : "keeps",
      "start_offset" : 33,
      "end_offset" : 38,
      "type" : "<ALPHANUM>",
      "position" : 8
    },
    {
      "token" : "doctor",
      "start_offset" : 39,
      "end_offset" : 45,
      "type" : "<ALPHANUM>",
      "position" : 9
    },
    {
      "token" : "away",
      "start_offset" : 46,
      "end_offset" : 50,
      "type" : "<ALPHANUM>",
      "position" : 10
    }
  ]
}
```

返回结果中的每一个`token`即为分词后的每一个单词，写时分词器需要在 mapping 中指定，而且一经指定就不能再修改，若要修改必须新建索引。 

### 3.2、读时分词

由于读时分词器默认与写时分词器默认保持一致，这种默认设定也是非常容易理解的，读写采用一致的分词器，才能尽最大可能保证分词的结果是可以匹配的。 

当然，ES 允许读时分词器单独设置：

```javascript
curl -H "Content-Type: application/json" -XPOST localhost:9200/my_test/_search?pretty -d '
{
    "query": {
        "match": {
            "title": {
                "query": "eating",         
                "analyzer": "my_analyzer"
            }
        }
    }
}'
```

如果不单独设置分词器，那么读时分词器的验证方法与写时一致；如果是自定义分词器，那么可以使用如下的 api 来自行验证结果：

```javascript
GET /_analyze

POST /_analyze

GET /<index>/_analyze

POST /<index>/_analyze
```

例如：

```javascript
curl -H "Content-Type: application/json" -XPOST localhost:9200/my_test/_analyze?pretty -d '
{
    "text":"eating",
    "analyzer":"english"
}'
```

 返回结果如下： 

```json
{
  "tokens" : [
    {
      "token" : "eat",
      "start_offset" : 0,
      "end_offset" : 6,
      "type" : "<ALPHANUM>",
      "position" : 0
    }
  ]
}
```

由上可知 `english`分词器会将 `eating`处理为 `eat`，用该分词器将查询不到结果，而使用自定义`my_analyzer`则可以：

```javascript
curl -H "Content-Type: application/json" -XPOST localhost:9200/my_test/_analyze?pretty -d '
{
    "text":"eating",
    "analyzer":"my_analyzer"
}'
```

结果为：

```json
{
  "tokens" : [
    {
      "token" : "eating",
      "start_offset" : 0,
      "end_offset" : 6,
      "type" : "<ALPHANUM>",
      "position" : 0
    }
  ]
}
```

