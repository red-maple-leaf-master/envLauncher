# env-launcher

## 技术列表
* javafx
* maven
* jdk17

使用jpackage打包,将jre运行环境也都打包到 exe 中,方便没有安装 Java环境的电脑运行

## 如何打包
在idea的 maven 插件框中找到javafx:jink进行打包 
然后通过run.bat进行打包成exe 或者在当前项目根目录的cmd窗口中 运行
```shell
jpackage --name envLauncher --type app-image -m top.oneyi.jdktool/top.oneyi.jdktool.MainApp --runtime-image .\target\app\
```
**参数说明**
```shell
--name 输出的exe文件名
--type app-image 输出的类型
--runtime-image 运行时环境
```
**注意**
> 需要将电脑的环境切换成jdk17 不然打包会报错 切换jdk的时候切记要重启idea
> 打包好的exe需要使用管理员运行 因为path设置的时候是使用的注册表的方式
