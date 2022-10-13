# Storm Example mod

## Mod-Project

### Setup

setGameDirectory will ask for the location of your Project Zomboid installation (see [capsid](https://github.com/pzstorm/capsid))

```
./gradlew setGameDirectory 
```

Then we are decompiling Project Zomboid classes to have them available for our disposition.

```
./gradlew zomboidClasses zomboidSourcesJar zomboidJar zomboidLuaJar
```

### Building your mod as zip file

```
./gradlew assembleDist
```


### What does this mod do?

This example mod prints "this is where your journey begins" in the console on a game start.