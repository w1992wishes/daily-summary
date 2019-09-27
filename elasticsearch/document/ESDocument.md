# 【ES】ElasticSearch 文档操作

[TOC]

## 一、索引文档

文档通过 index API 被索引--使数据可以被存储和搜索。，文档通过其 _index、 _type 、 _id 唯一确定。可以自己提供一个 _id ，或者也使用 index API 生成一个。

### 1.1、使用自己的 ID

如果文档有自然的标识符（例如user_account字段或者其他值表示文档），就可以提供自己的_id，使用这种形式的indexAPI：

```shell
PUT	/{index}/{type}/{id}
{
    "field": "value",
    ...
}
```

例如索引叫做 “website” ，类型叫做 “blog” ， ID 是 “123” ，那么这个索引请求就像这样：

```shell
curl -H "Content-Type: application/json" -XPUT localhost:9200/website/blog/123 -d '
{
    "title": "My first blog entry",
    "text": "Just trying this out...",
    "date": "2014/01/01"
}'
```

Elasticsearch 的响应：

```json
{
    "_index": "website",
    "_type": "blog",
    "_id": "123",
    "_version": 1,
    "result": "created"
}
```

响应指出请求的索引已经被成功创建，这个索引中包含 _index 、 _type 和 _id 元数据，以及一个新元素： _version 。Elasticsearch中 每个文档都有版本号，每当文档变化（包括删除）都会使 _version 增加。

### 1.2、自增 ID

如果数据没有自然 ID，可以让 Elasticsearch 自动生成。请求结构发生了变化： PUT 方法（“在这个 URL 中存储文档”，可以认为是更新） 变成了 POST 方法（“在这个类型下存储文档”，符合 POST 新增的语义）。

URL 现在只包含 _index 和 _type 两个字段：

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog -d '
{
    "title": "My second blog entry",
    "text": "Still trying this out...",
    "date": "2014/01/01"
}'
```

响应内容与刚才类似，只有 _id 字段变成了自动生成的值：

```json
{
    "_index": "website",
    "_type": "blog",
    "_id": "zqIzcm0BgOKExS3TRFoX",
    "_version": 1,
    "result": "created"
}
```

### 1.3、创建一个新文档

当索引一个文档，如何确定是完全创建了一个新的还是覆盖了一个已经存在的呢？

_index 、 _type 、 _id 三者唯一确定一个文档。所以要想保证文档是新加入的，最简单的方式是使用 POST 方法让 Elasticsearch 自动生成唯一 _id ：

```shell
POST /website/blog/
```

如果想使用自定义的 _id ，必须告诉 Elasticsearch 应该在 _index 、 _type 、 _id 三者都不同时才接受请求。为了做到这点有两种方法：

1. 第一种方法使用 op_type 查询参数：

   ```shell
   PUT /website/blog/123?op_type=create
   ```

2. 第二种方法是在 URL 后加 /_create 做为端点：

   ```shell
   PUT /website/blog/123/_create
   ```

如果成功创建了一个新文档，Elasticsearch 将返回正常的元数据且响应状态码是	201 Created 。

如果包含相同的 _index 、 _type 和 _id 的文档已经存在，Elasticsearch 将返
回 409 Conflict 响应状态码，并提示 `document already exists`。

## 二、检索文档

### 2.1、检索文档的全部

想要从 Elasticsearch 中获取文档，使用同样的 _index、 _type、 _id，但是 HTTP 方法改为 GET （在任意的查询字符串中增加 pretty 参数，会让 Elasticsearch 美化输出 (pretty-print) JSON 响应以便更加容易阅读）：

```shell
curl -XGET localhost:9200/website/blog/123?pretty
```

响应包含了元数据节点，增加了 _source 字段，它包含了在创建索引时发送给Elasticsearch 的原始文档：

```json
{
  "_index" : "website",
  "_type" : "blog",
  "_id" : "123",
  "_version" : 1,
  "found" : true,
  "_source" : {
    "title" : "My first blog entry",
    "text" : "Just trying this out...",
    "date" : "2014/01/01"
  }
}
```

### 2.2、检索文档的一部分

通常，GET 请求将返回文档的全部，存储在 _source 参数中。但是可能感兴趣的字段只是title。请求个别字段可以使用 _source 参数。多个字段可以使用逗号分隔：

```shell
curl -i -XGET localhost:9200/website/blog/123?_source=title,text
```

-i 可以显示请求头，_source 字段现在只包含请求的字段，过滤了date字段：

```json
HTTP/1.1 200 OK
content-type: application/json; charset=UTF-8
content-length: 148
{
    "_index": "website",
    "_type": "blog",
    "_id": "123",
    "_version": 1,
    "found": true,
    "_source": {
        "text": "Just trying this out...",
        "title": "My first blog entry"
    }
}
```

或者只想得到 _source 字段而不要其他的元数据，可以这样请求：

```shell
curl -XGET localhost:9200/website/blog/123/_source?pretty
```

它仅仅返回:

```json
{
  "title" : "My first blog entry",
  "text" : "Just trying this out...",
  "date" : "2014/01/01"
}
```

### 2.3、检查文档是否存在

如果想做的只是检查文档是否存在，使用 HEAD 方法来代替 GET 。 HEAD 请求不会返回响应体，只有HTTP头：

```SHELL
curl --head http://localhost:9200/website/blog/123
```

Elasticsearch 将会返回 200 OK 状态如果文档存在：

```json
HTTP/1.1 200 OK
content-type: application/json; charset=UTF-8
content-length: 188
```

### 2.4、检索多个文档

检索多个文档依旧非常快。合并多个请求可以避免每个请求单独的网络开销。如果需要从 Elasticsearch 中检索多个文档，相对于一个一个的检索，更快的方式是在一个请求中使用 multi-get 或者 mget API。

mgetAPI 参数是一个 docs 数组，数组的每个节点定义一个文档的 _index、 _type、 _id 元数据。如果只想检索一个或几个确定的字段，也可以定义一个 _source 参数：

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/_mget?pretty -d '
{
    "docs": [
        {
            "_index": "website",
            "_type": "blog",
            "_id": 1
        },
        {
            "_index": "website",
            "_type": "blog",
            "_id": 2,
            "_source": "views"
        }
    ]
}'
```

响应体也包含一个 docs 数组，每个文档还包含一个响应，它们按照请求定义的顺序排列。

```json
{
  "docs" : [
    {
      "_index" : "website",
      "_type" : "blog",
      "_id" : "1",
      "found" : false
    },
    {
      "_index" : "website",
      "_type" : "blog",
      "_id" : "2",
      "_version" : 3,
      "found" : true,
      "_source" : {
        "views" : 3
      }
    }
  ]
}
```

如果想检索的文档在同一个 `_index`中（甚至在同一个 `_type` 中），就可以在 URL 中定义一个默认的 `/_index` 或者  `/_index/_type` :

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/_mget?pretty -d '
{
    "docs": [
        {
            "_id": 1
        },
        {
            "_type": "pageviews", "_id": 1
        }
    ]
}'
```

如果所有文档具有相同 _index 和 _type，可以通过简单的 ids 数组来代替完整的 docs 数组：

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/_mget?pretty -d '
{
    "ids": ["2", "1"]
}'
```

## 三、更新文档

### 3.1、整体文档更新

文档在 Elasticsearch 中是不可变的--不能修改他们。如果需要更新已存在的文档，可以重建索引(reindex)，或者替换掉它。

```shell
curl -H "Content-Type: application/json" -XPUT localhost:9200/website/blog/123?pretty -d '
{
    "title": "My first blog entry",
    "text": "I am starting to get the hang of this...",
    "date": "2014/01/02"
}'
```

在响应中，可以看到 Elasticsearch 把 _version 增加了:

```json
{
  "_index" : "website",
  "_type" : "blog",
  "_id" : "123",
  "_version" : 2,
  "result" : "updated"
}
```

在内部，Elasticsearch 已经标记旧文档为删除并添加了一个完整的新文档。旧版本文档不会立即消失，但也不能去访问它。Elasticsearch 会在继续索引更多数据时清理被删除的文档。

### 3.2、指定版本更新文档

Elasticsearch 是分布式的。当文档被创建、更新或删除，文档的新版本会被复制到集群的其它节点。Elasticsearch 即是同步的又是异步的，意思是这些复制请求都是平行发送的，并无序(out of sequence)的到达目的地。这就需要一种方法确保老版本的文档永远不会覆盖新的版本。

执行 index 、 get 、 delete 请求时，每个文档都有一个 _version 号码，这个号码在文档被改变时加一。Elasticsearch 使用这个 _version 保证所有修改都被正确排序。当一个旧版本出现在新版本之后，它会被简单的忽略。

利用 _version 的这一优点确保数据不会因为修改冲突而丢失。可以指定文档的	version 来做想要的更改。如果那个版本号不是现在的，请求就失败了。

创建一个新的博文：

```shell
curl -H "Content-Type: application/json" -XPUT localhost:9200/website/blog/1/_create -d '
{
    "title": "My first blog entry",
    "text": "Just trying this out..."
}'
```

首先检索文档：

```shell
curl -XGET localhost:9200/website/blog/1?pretty
```

响应体包含相同的 _version 是 1：

```json
{
  "_index" : "website",
  "_type" : "blog",
  "_id" : "1",
  "_version" : 1,
  "found" : true,
  "_source" : {
    "title" : "My first blog entry",
    "text" : "Just trying this out..."
  }
}
```

现在，当通过重新索引文档保存修改时，这样指定了 version 参数，只希望文档的_version 是 1 时更新才生效：

```shell
curl -H "Content-Type: application/json" -XPUT localhost:9200/website/blog/1?version=1 -d '
{
    "title": "My first blog entry",
    "text": "Starting to get the hang of this..."
}'
```

请求成功，响应体_version 已经增加到 2 ：

```json
{
    "_index": "website",
    "_type": "blog",
    "_id": "1",
    "_version": 2,
    "result": "updated"
}
```

如果重新运行相同的索引请求，依旧指定 version=1，Elasticsearch 将返回 409
Conflict 状态的 HTTP 响应。

### 3.3、文档局部更新

通过检索，修改，然后重建整文档的索引方法来更新文档。

使用 update API，可以使用一个请求来实现局部更新，用于添加新字段或者更新已有字段。

文档是不可变的--它们不能被更改，只能被替换。 update API 必须遵循相同的规则。表面看来，似乎是局部更新了文档的位置，内部却是像之前说的一样简单使用update API 处理相同的检索-修改-重建索引流程，也减少了其他进程可能导致冲突的修改。

最简单的 update 请求表单接受一个局部文档参数 doc ，它会合并到现有文档中--对象合并在一起，存在的标量字段被覆盖，新字段被添加。

可以使用以下请求为博客添加一个 tags 字段和一个 views 字段：

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/1/_update?pretty -d '
{
    "doc": {
        "tags": [
            "testing"
        ],
        "views": 0
    }
}'
```

如果请求成功，将看到类似 index 请求的响应结果：

```json
{
  "_index" : "website",
  "_type" : "blog",
  "_id" : "1",
  "_version" : 3,
  "result" : "updated"
```

检索文档文档显示被更新的 _source 字段：

```json
{
  "_index" : "website",
  "_type" : "blog",
  "_id" : "1",
  "_version" : 3,
  "found" : true,
  "_source" : {
    "title" : "My first blog entry",
    "text" : "Starting to get the hang of this...",
    "views" : 0,
    "tags" : [
      "testing"
    ]
  }
}

```

### 3.4、使用脚本局部更新

当 API 不能满足要求时，Elasticsearch 允许使用脚本实现自己的逻辑。

默认的脚本语言是 Groovy，一个快速且功能丰富的脚本语言，语法类似于Javascript。

脚本能够使用 update API 改变 ` _source` 字段的内容，它在脚本内部以 `ctx._source` 表示。

可以使用脚本增加博客的 views 数量：

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/1/_update?pretty -d '
{
    "script": "ctx._source.views+=1"
}'
```

还可以使用脚本增加一个新标签到 tags 数组中：

```json
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/1/_update?pretty -d '
{
    "script": {
        "source": "ctx._source.tags.add(params.new_tag)",
        "params": {
            "new_tag": "search"
        }
    }
}'
```

### 3.5、更新可能不存在的文档

要在 Elasticsearch 中存储浏览量计数器。每当有用户访问页面，增加这个页面的浏览量。但如果这是个新页面，并不确定这个计数器存在与否，当试图更新一个
不存在的文档，更新将失败。

可以使用 upsert 参数定义文档来使其不存在时被创建。

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/2/_update?pretty -d '
{
    "script": "ctx._source.views+=1",
    "upsert": {
        "views": 1
    }
}'
```

第一次执行这个请求， upsert 值被索引为一个新文档，初始化 views 字段为 1，接下来文档已经存在，所以 script 被更新代替，增加 views 数量。

对于多用户的局部更新，两个进程都要增加页面浏览量，增加的顺序可以不关心，如果冲突发生，可以重新尝试更新既可。

可以通过 retry_on_conflict 参数设置重试次数来自动完成，这样 update 操作将会在发生错误前重试--这个值默认为0。

```shell
curl -H "Content-Type: application/json" -XPOST localhost:9200/website/blog/2/_update?retry_on_conflict=5 -d '
{
    "script": "ctx._source.views+=1",
    "upsert": {
        "views": 0
    }
}'
```

### 3.6、更新时的批量操作

就像 mget 允许一次性检索多个文档一样， bulk API允许使用单一请求来实现多个文档的 create、 index、 update 或 delete。这对索引类似于日志活动这样的数据流非常有用，它们可以以成百上千的数据为一个批次按序进行索引。

bulk 请求体如下，它有一点不同寻常：

这种格式类似于用 "\n" 符号连接起来的一行一行的 JSON 文档流(stream)。
两个重要的点需要注意：

```json
{ action: { metadata }}\n
{ request body }\n
{ action: { metadata }}\n
{ request body }\n
...
```

这种格式类似于用 "\n" 符号连接起来的一行一行的 JSON 文档流(stream)。
两个重要的点需要注意：

- 每行必须以 "\n" 符号结尾，包括最后一行。这些都是作为每行有效的分离而做的标记。
- 每一行的数据不能包含未被转义的换行符，它们会干扰分析--这意味着 JSON 不能被美化打印。

#### 3.6.1、action/metadata

这一行定义了文档行为(what action)发生在哪个文档(which document)之上。

行为(action)必须是以下几种：

- create：当文档不存在时创建
- index：创建新文档或替换已有文档
- update：局部更新文档
- delete：删除一个文档

在索引、创建、更新或删除时必须指定文档的`_index、 _type、 _id`这些元数据(metadata)。

例如删除请求看起来像这样：

```json
{ "delete": { "_index": "website", "_type": "blog", "_id": "123" }}
```

#### 3.6.2、请求体(request body)

由文档的 `_source` 组成--文档所包含的一些字段以及其值。

- 它被 index 和 create 操作所必须，这是有道理的：必须提供文档用来索引
- 这些还被 update 操作所必需，而且请求体的组成应该与 update API（ doc, upsert, script 等等）一致
- 删除操作不需要请求体(request body)

```json
{ "create": { "_index": "website", "_type": "blog", "_id": "123" }}
{ "title": "My first blog post" }
```

#### 3.6.3、bulk 请求

为了将这些放在一起，bulk 请求表单是这样的：

```json
curl -H "Content-Type: application/json" -XPOST localhost:9200/_bulk?pretty -d '
{ "delete": { "_index": "website", "_type": "blog", "_id": "123" }}
{ "create": { "_index": "website", "_type": "blog", "_id": "123" }}
{ "title":    "My first blog post" }
{ "index":  { "_index": "website", "_type": "blog" }}
{ "title":    "My second blog post" }
{ "update": { "_index": "website", "_type": "blog", "_id": "123", "_retry_on_conflict" : 5}}
{ "doc" : {"title" : "My updated blog post"}}
'
```

Elasticsearch 响应包含一个 items 数组，它罗列了每一个请求的结果，结果的顺序与我们请求的顺序相同：

```json
{
  "took" : 1069,
  "errors" : false,
  "items" : [
    {
      "delete" : {
        "_index" : "website",
        "_type" : "blog",
        "_id" : "123",
        "_version" : 1,
        "result" : "not_found",
        "_shards" : {
          "total" : 1,
          "successful" : 1,
          "failed" : 0
        },
        "_seq_no" : 0,
        "_primary_term" : 1,
        "status" : 404
      }
    },
    {
      "create" : {
        "_index" : "website",
        "_type" : "blog",
        "_id" : "123",
        "_version" : 2,
        "result" : "created",
        "_shards" : {
          "total" : 1,
          "successful" : 1,
          "failed" : 0
        },
        "_seq_no" : 1,
        "_primary_term" : 1,
        "status" : 201
      }
    },
    {
      "index" : {
        "_index" : "website",
        "_type" : "blog",
        "_id" : "_--jcm0Bsjr6Q3VWz7kw",
        "_version" : 1,
        "result" : "created",
        "_shards" : {
          "total" : 1,
          "successful" : 1,
          "failed" : 0
        },
        "_seq_no" : 0,
        "_primary_term" : 1,
        "status" : 201
      }
    },
    {
      "update" : {
        "_index" : "website",
        "_type" : "blog",
        "_id" : "123",
        "_version" : 3,
        "result" : "updated",
        "_shards" : {
          "total" : 1,
          "successful" : 1,
          "failed" : 0
        },
        "_seq_no" : 2,
        "_primary_term" : 1,
        "status" : 200
      }
    }
  ]
}
```

每个子请求都被独立的执行，所以一个子请求的错误并不影响其它请求。如果任何一个请求失败，顶层的 error 标记将被设置为 true，然后错误的细节将在相应的请求中被报告。

这些说明 bulk 请求不是原子操作--它们不能实现事务。每个请求操作时分开的，所以每个请求的成功与否不干扰其它操作。

为每个文档指定相同的元数据是多余的。就像 mget  API，bulk 请求也可以在 URL 中使用 `/_index` 或 `/_index/_type` :

```shell
POST /website/_bulk
{ "index": { "_type": "log" }}
{ "event": "User logged in" }
```

依旧可以覆盖元数据行的 `_index` 和 `_type` ，在没有覆盖时它会使用 URL 中的值作为默认值：

```json
POST /website/log/_bulk
{ "index": {}}
{ "event": "User logged in" }
{ "index": { "_type": "blog" }}
{ "title": "Overriding the default type" }
```

## 四、删除文档

删除文档的语法模式与之前基本一致，只不过要使用 DELETE 方法：

```shell
curl -XDELETE localhost:9200/website/blog/123
```

如果文档被找到，Elasticsearch 将返回 200 OK 状态码和以下响应体。注意	_version	数字已经增加了：

```json
{
    "_index": "website",
    "_type": "blog",
    "_id": "123",
    "_version": 3,
    "result": "deleted"
}
```

