# 数据结构-二叉树

树 是一种经常用到的数据结构，用来模拟具有树状结构性质的数据集合。

树里的每一个节点有一个根植和一个包含所有子节点的列表。从图的观点来看，树也可视为一个拥有N 个节点和N-1 条边的一个有向无环图。

二叉树是一种更为典型的树树状结构。如它名字所描述的那样，二叉树是每个节点最多有两个子树的树结构，通常子树被称作“左子树”和“右子树”。

## 一、树的遍历 

* 前序遍历：前序遍历首先访问根节点，然后遍历左子树，最后遍历右子树。
* 中序遍历：中序遍历是先遍历左子树，然后访问根节点，然后遍历右子树。对于二叉搜索树，我们可以通过中序遍历得到一个递增的有序序列。
* 后序遍历：后序遍历是先遍历左子树，然后遍历右子树，最后访问树的根节点。

### 1.1、PreorderTraversal

Given a binary tree, return the preorder traversal of its nodes' values.

Example:

Input: [1,null,2,3]
```
   1
    \
     2
    /
   3
```
Output: [1,2,3]

给定一个二叉树，返回它的 前序 遍历。

### 1.2、InorderTraversal

Given a binary tree, return the inorder traversal of its nodes' values.

Example:

Input: [1,null,2,3]
```
   1
    \
     2
    /
   3
```
Output: [1,3,2]

给定一个二叉树，返回它的中序 遍历。

### 1.3、PostorderTraversal

Given a binary tree, return the postorder traversal of its nodes' values.

Example:

Input: [1,null,2,3]
```
   1
    \
     2
    /
   3
```
Output: [3,2,1]

给定一个二叉树，返回它的 后序 遍历。

### 1.4、层序遍历

层序遍历就是逐层遍历树结构。

广度优先搜索是一种广泛运用在树或图这类数据结构中，遍历或搜索的算法。 该算法从一个根节点开始，首先访问节点本身。 然后遍历它的相邻节点，其次遍历它的二级邻节点、三级邻节点，以此类推。

当我们在树中进行广度优先搜索时，我们访问的节点的顺序是按照层序遍历顺序的。

**LevelOrderTraversal**

Given a binary tree, return the level order traversal of its nodes' values. (ie, from left to right, level by level).

For example:
Given binary tree [3,9,20,null,null,15,7],
```
    3
   / \
  9  20
    /  \
   15   7
```

return its level order traversal as:
```
[
  [3],
  [9,20],
  [15,7]
]
```

## 二、运用递归解决树的问题

树可以以递归的方式定义为一个节点（根节点），它包括一个值和一个指向其他节点指针的列表。 递归是树的特性之一。 因此，许多树问题可以通过递归的方式来解决。 对于每个递归层级，我们只能关注单个节点内的问题，并通过递归调用函数来解决其子节点问题。

通常，可以通过 “自顶向下” 或 “自底向上” 的递归来解决树问题。

### 2.1、“自顶向下” 的解决方案

“自顶向下” 意味着在每个递归层级，我们将首先访问节点来计算一些值，并在递归调用函数时将这些值传递到子节点。 所以 “自顶向下” 的解决方案可以被认为是一种前序遍历。

具体来说，递归函数 top_down(root, params) 的原理是这样的：

```
1. return specific value for null node
2. update the answer if needed                      // anwer <-- params
3. left_ans = top_down(root.left, left_params)      // left_params <-- root.val, params
4. right_ans = top_down(root.right, right_params)   // right_params <-- root.val, params
5. return the answer if needed                      // answer <-- left_ans, right_ans
```

例如，思考这样一个问题：给定一个二叉树，请寻找它的最大深度。

我们知道根节点的深度是1。 对于每个节点，如果我们知道某节点的深度，那我们将知道它子节点的深度。 因此，在调用递归函数的时候，将节点的深度传递为一个参数，那么所有的节点都知道它们自身的深度。 而对于叶节点，我们可以通过更新深度从而获取最终答案。 这里是递归函数 maximum_depth(root, depth) 的伪代码：

```
1. return if root is null
2. if root is a leaf node:
3.      answer = max(answer, depth)         // update the answer if needed
4. maximum_depth(root.left, depth + 1)      // call the function recursively for left child
5. maximum_depth(root.right, depth + 1)     // call the function recursively for right child
```

### 2.2、“自底向上” 的解决方案

“自底向上” 是另一种递归方法。 在每个递归层次上，首先对所有子节点递归地调用函数，然后根据返回值和根节点本身的值得到答案。 这个过程可以看作是后序遍历的一种。 通常， “自底向上” 的递归函数 bottom_up(root) 为如下所示：

```
1. return specific value for null node
2. left_ans = bottom_up(root.left)          // call function recursively for left child
3. right_ans = bottom_up(root.right)        // call function recursively for right child
4. return answers 
```

继续讨论前面关于树的最大深度的问题，但是使用不同的思维方式：对于树的单个节点，以节点自身为根的子树的最大深度x是多少？

如果我们知道一个根节点，以其左子节点为根的最大深度为l和以其右子节点为根的最大深度为r，我们是否可以回答前面的问题？ 当然可以，我们可以选择它们之间的最大值，再加上1来获得根节点所在的子树的最大深度。 那就是 x = max（l，r）+ 1。

这意味着对于每一个节点来说，我们都可以在解决它子节点的问题之后得到答案。 因此，我们可以使用“自底向上“的方法。下面是递归函数 maximum_depth(root) 的伪代码：

```
1. return 0 if root is null                 // return 0 for null node
2. left_depth = maximum_depth(root.left)
3. right_depth = maximum_depth(root.right)
4. return max(left_depth, right_depth) + 1  // return depth of the subtree rooted at root
```

### 2.3、总结

当遇到树问题时，先思考一下两个问题：

能确定一些参数，从该节点自身解决出发寻找答案吗？
可以使用这些参数和节点本身的值来决定什么应该是传递给它子节点的参数吗？
如果答案都是肯定的，那么请尝试使用 “自顶向下” 的递归来解决此问题。

或者可以这样思考：
对于树中的任意一个节点，如果知道它子节点的答案，能计算出该节点的答案吗？ 
如果答案是肯定的，那么 “自底向上” 的递归可能是一个不错的解决方法。

### 2.4、Maximum Depth of Binary Tree

Given a binary tree, find its maximum depth.

The maximum depth is the number of nodes along the longest path from the root node down to the farthest leaf node.

Note: A leaf is a node with no children.

Example:

Given binary tree [3,9,20,null,null,15,7],

```
    3
   / \
  9  20
    /  \
   15   7
```
return its depth = 3.

给定一个二叉树，找出其最大深度。

二叉树的深度为根节点到最远叶子节点的最长路径上的节点数。

说明: 叶子节点是指没有子节点的节点。

### 2.5、Symmetric Tree （对称二叉树）

Given a binary tree, check whether it is a mirror of itself (ie, symmetric around its center).

For example, this binary tree [1,2,2,3,4,4,3] is symmetric:

```
    1
   / \
  2   2
 / \ / \
3  4 4  3
```

But the following [1,2,2,null,3,null,3] is not:

```
    1
   / \
  2   2
   \   \
   3    3
```

Note:
Bonus points if you could solve it both recursively and iteratively.

给定一个二叉树，检查它是否是镜像对称的。

如果你可以运用递归和迭代两种方法解决这个问题，会很加分。

### 2.6、Path Sum

Given a binary tree and a sum, determine if the tree has a root-to-leaf path such that adding up all the values along the path equals the given sum.

Note: A leaf is a node with no children.

Example:

Given the below binary tree and sum = 22,

```
      5
     / \
    4   8
   /   / \
  11  13  4
 /  \      \
7    2      1
```

return true, as there exist a root-to-leaf path 5->4->11->2 which sum is 22.