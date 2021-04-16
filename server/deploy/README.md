# Deploy configuration during development:

Instead of pushing compiling and testing everything on a local machine,
these set of scripts send the code changes to a remote machine.
Those are then compile remotely

## watchdog.sh:
Watches for file changes and automatically calls sync.sh

## sync.sh
Wrapper over push_code.sh
Also makes a curl request to trigger an automatic recompilation of the sources (sbt).

## push_code.sh
Pushes any relevant Scala or JS code.

# TROUBLESHOOTING 
## \r windows issue
```
sed -i 's/\r$//' *.sh
# ignore new changes in commits 
git update-index --assume-unchanged *.sh
```


