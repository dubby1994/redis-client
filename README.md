# Redis Client

使用JavaFX，不依赖任何其他依赖，实现的一个简单的Redis客户端，编写的初衷是觉得Redis Desktop Manager太难用，并且Redis的RESP比较简单，所以这里就尝试着写了下

>考虑到这个主要用途是本地使用，所以没有使用长连接，因为我本人经常是打开Client就一直开着，但一般很少会用到，所以每次query都会新建一个socket连接

+ 不支持Redis Cluster
+ 不支持sub/pub和部分堵塞命令