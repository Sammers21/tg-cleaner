# tg-cleaner
Automatically cleans up things you don't want to appear in a telegram chat 

# Build and run instructions

1. Build tdlib, by following steps from [the instruction](https://tdlib.github.io/td/build.html?language=Java)
2. Make it accessible in you PATH
3. Build the actual CLI programm: `./gradlew fatJar`
4. Run resulted `.jar` file: `java -jar build/libs/tg-cleaner.jar` 
5. Input your telephone number and received code.
6. Now listed below features will work.

# Features

`#tgc_ignore` - ignores specific sticker 

![demo](https://user-images.githubusercontent.com/16746106/64022539-24a9d980-cb3f-11e9-98cb-c69d67d22214.gif)

`#tgc_allow_text_only` - ignores anything other then a text message

![allow_text_demo](https://user-images.githubusercontent.com/16746106/64022906-dcd78200-cb3f-11e9-9e61-5b282a5337c2.gif)
