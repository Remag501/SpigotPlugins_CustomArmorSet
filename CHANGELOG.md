# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## 1.1.0 (2026-02-20)


### Features

* added BGSCore API to plugin ([451d551](https://github.com/Battlegrounds-Development/armor/commit/451d5516078a1538d835cbf21ea929add703c829))
* Created a namespace service to keep track of namespaces ([5bfc291](https://github.com/Battlegrounds-Development/armor/commit/5bfc291288666065229ec620343493e739b36b47))
* made all files under manager instance friendly ([59f0bbb](https://github.com/Battlegrounds-Development/armor/commit/59f0bbbad41ce399e761b38467df4f5ba52d8744))
* registered /bgs armor to armor set commands ([a542f35](https://github.com/Battlegrounds-Development/armor/commit/a542f3517120d4c20f1a16897d3682ea7ccba921))
* updated plugin to 1.21.8 ([1761ab1](https://github.com/Battlegrounds-Development/armor/commit/1761ab1d6b9ebc4c6be8010085089fa1c8310353))


### Bug Fixes

* armor manager uses task services instead of plugin to run helmet equip later ([2073acd](https://github.com/Battlegrounds-Development/armor/commit/2073acda491869f2f1901412c92f4fd465561f42))
* armor util references namespace service to prevent hardcoding (bug: armor points does not work on new version) ([62d2c73](https://github.com/Battlegrounds-Development/armor/commit/62d2c734d0621359161243cf80f05d36a6727ec9))
* attribute service values are now match expectations of core ([f13c7d5](https://github.com/Battlegrounds-Development/armor/commit/f13c7d50be2eb96f58c8697eeec9a268fa8e57f4))
* cleaned up snowman since the set will currently leak into other listeners. ([e48329b](https://github.com/Battlegrounds-Development/armor/commit/e48329b39ca4512609d792dc467972a0ff3de3ac))
* fixed implementation bugs with bgs core ([8263812](https://github.com/Battlegrounds-Development/armor/commit/82638128aa2a7c5ec34884108a63ece352f7a9b1))
* general fixes on armor sets and abilityService implementation ([4c701ed](https://github.com/Battlegrounds-Development/armor/commit/4c701eda357a5cc5ed7dd9d390e684f6c2bfdb35))
* renamed particle and attribute references to match 1.21 ([5d0c537](https://github.com/Battlegrounds-Development/armor/commit/5d0c537b0ca3d8a450ab644ed4981a886a41df5b))
