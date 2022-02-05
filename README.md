# elf-reader
Утилита для чтения и дизассемблирования ELF-файлов. 
Выполнена в качестве одной из домашних работ к Архитектуре ЭВМ 1 курса КТ ИТМО.

Доступно декодирование заголовка ELF-файла, таблицы секций и таблицы меток.

Поддерживается дизассемблирование только RISC-V 32I/M/C архитектуры.

Также приложены [Отчёт](https://github.com/belous-dp/elf-reader/blob/main/%D0%9E%D1%82%D1%87%D1%91%D1%82.pdf), несколько файлов для тестирования и результат работы программы на них.


## Использование
### Откомпилированный архив
`java -jar elfreader.jar [аргументы] <имя входного файла> [имя выходного файла]`
### Из исходников
`javac -d bin -encoding utf8 src/elf/*.java src/riscv/*.java src/Main.java`

`java -cp ./bin Main [аргументы] <имя входного файла> [имя выходного файла]`

Пример: `java -cp ./bin Main -i -a tests/test1.elf`

### Аргументы/флаги
  * `-h`, `--help` — отобразить текущее сообщение
  * `-i`, `--inline` — выбрать режим вывода в консоль. В этом случае имя выходного файла будет игнорироваться
  * `-H`, `--file-header` — выводить заголовок файла
  * `-S`, `--section-headers` — выводить таблицу секций
  * `-s`, `--symtab` — выводить таблицу меток
  * `-t`, `--text` — дизассемблировать и выводить секцию .text
  * `-a`, `--all` — то же самое, что `-H -S -s -t`
  * `-q`, `--quiet` — не бросать некритические исключения, а выводить их в файл

