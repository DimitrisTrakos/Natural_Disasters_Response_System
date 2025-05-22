javac -cp "lib/jade.jar:out" -d out/ src/**/*.java

if [ $# -eq 0 ]; then
    java -cp "lib/jade.jar:out" main.Main
else
    java -cp "lib/jade.jar:out" main.Main "$@"
fi