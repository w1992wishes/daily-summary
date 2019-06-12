# 数据结构-二叉树

树 是一种经常用到的数据结构，用来模拟具有树状结构性质的数据集合。

树里的每一个节点有一个根植和一个包含所有子节点的列表。从图的观点来看，树也可视为一个拥有N 个节点和N-1 条边的一个有向无环图。

二叉树是一种更为典型的树树状结构。如它名字所描述的那样，二叉树是每个节点最多有两个子树的树结构，通常子树被称作“左子树”和“右子树”。

## 一、树的遍历 

* 前序遍历：前序遍历首先访问根节点，然后遍历左子树，最后遍历右子树。
* 中序遍历：中序遍历是先遍历左子树，然后访问根节点，然后遍历右子树。对于二叉搜索树，我们可以通过中序遍历得到一个递增的有序序列。
* 后序遍历：后序遍历是先遍历左子树，然后遍历右子树，最后访问树的根节点。

### PreorderTraversal

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

### InorderTraversal

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

### PostorderTraversal

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

## 二、层序遍历

层序遍历就是逐层遍历树结构。

广度优先搜索是一种广泛运用在树或图这类数据结构中，遍历或搜索的算法。 该算法从一个根节点开始，首先访问节点本身。 然后遍历它的相邻节点，其次遍历它的二级邻节点、三级邻节点，以此类推。

当我们在树中进行广度优先搜索时，我们访问的节点的顺序是按照层序遍历顺序的。

### LevelOrderTraversal

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