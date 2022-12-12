# zProl

Z Programming Language

For actual usage check the [wiki](https://github.com/EpicPix/zProl/wiki).

Ideas/Proposals for this language create at https://zprol.epicpix.ga/

You can also run zProl online on https://zprol.epicpix.ga/runner

## Self Hosting

*Updating zProl to be completely self-hosted might <ins>add or remove features</ins>, which might include rewriting the whole `zpil` format*

Current progress:
- [x] Lexing
  - [x] Error Support
  - [x] Tokens
    - [x] Identifiers
      - [x] Keywords
    - [x] Operators
    - [x] Comments
    - [x] Whitespace
    - [x] Numbers
    - [x] String
- [ ] Parsing
- [ ] Compiling
- [ ] `zpil` format
- [ ] Target Generation
  - [ ] x86_64 Assembly for Linux

## HTTP Api

You can run zProl online using an api which is on https://zprol.epicpix.ga/api/v1/run

You have to use the `POST` method with the `Content-Type` set to `application/json`.

There must be either `code` or `code_url` defined in the json.
Optional values:
- when `debug` is set to `true` then it disables optimizations.
- when `get_assembly` is set to `true` then instead of running the code, the api provides the assembly code for x86_64

The program result will be in `run` and in `commit` there will be the currently running commit.

## Maven Repository

To use parts of this project like `zprol-parser` you can use my repository

```xml
<repositories>
    <repository>
        <id>epicpix</id>
        <name>EpicPix</name>
        <url>https://maven.epicpix.ga/releases/</url>
    </repository>
</repositories>
```

and then to use `zprol-parser` you can do

```xml
<dependencies>
    <dependency>
        <groupId>ga.epicpix</groupId>
        <artifactId>zprol-parser</artifactId>
        <version>1.0.8</version>
    </dependency>
</dependencies>
```

To check all other available modules you can check my [maven repository](https://maven.epicpix.ga/)