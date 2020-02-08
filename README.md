# tg-cleaner

The app behaves just like a telegram client on your mobile/desktop
computer alowing you to delete anything other than a text
messages or deleting a specific sticker in an automatic way.

Reply to a sticker you want to be ignored with `#tgc_ignore` command: 

![demo](https://user-images.githubusercontent.com/16746106/64022539-24a9d980-cb3f-11e9-98cb-c69d67d22214.gif)

`#tgc_allow_text_only` togles text-only mode in a private chat:

![allow_text_demo](https://user-images.githubusercontent.com/16746106/64022906-dcd78200-cb3f-11e9-9e61-5b282a5337c2.gif)


# Docker image

To run the docker image, execute:

`$ docker run --restart always --name tg-cleaner -v $(pwd):/tdlib -d -it sammers/tg-cleaner:1.0`

_/my/own/dir_ should be replaced with a path on your computer.

Then you can enter your Telegram account: 

`$ docker exec -it tg-cleaner tmux a`

Input your telephone number, received code and a cloud password if needed. Then just quit terminal(NOT USING CNTRL+C).


# Build and run on-host instructions

1. Build tdlib, by following steps from [the instruction](https://tdlib.github.io/td/build.html?language=Java)
2. Make it accessible in you PATH
3. Build the actual CLI programm: `./gradlew fatJar`
4. Run resulted `.jar` file: `java -jar build/libs/tg-cleaner.jar` 
5. Input your telephone number and received code.
6. Now listed below features will work.
