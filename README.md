# libhwp-server

이 프로젝트는 [libhwp](https://github.com/accforaus/libhwp)를 이용한 간단한 spring-boot api 서버 입니다. 

## Warning

This project is too simple to service for another project.\
Just use reference how to ues [libhwp](https://github.com/accforaus/libhwp)

enjoy!

## Usage

```
git clone https://github.com/accforaus/libhwp-server.git
cd libhwp-server
./gradlew bootrun

...
```

## Services

- `storeHWPFile` - store hwp files in server directory
- `loadZipAsResource` - get resource that to compressed multiple hwp files
- `loadHwpAsResource` - get resource that single hwp file
- `getBinPath` - get binary image path for single hwp file
- `getNormalText` - get extracted normal text in single hwp file
- `getTable` - get extracted table in single hwp file
- `attachFile` - attach multiple hwp files
- `getFileNameChaged` - change hwp file name
- `extractImage` - extract images in hwp file