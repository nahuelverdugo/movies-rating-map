## Movie Rating Map
Here is the [demo](https://nahuelvr.github.io/movies-rating-map/).
### Step 1
The reviews are extracted by using Maven into a CSV file.
```
$ cd movies-scraper
$ mvn clean install -Dmaven.test.skip=true
$ cd target
$ nohup java -jar filmAffinity-jar-with-dependencies.jar &
```
It will generate a file called `data.csv`.
### Step 2
The JSON object needed in the web interface is created running the following SBT command:
```
$ cd movies-spark
$ sbt run
```
### Step 3
Finally, the React application is built and run with the use of NPM.
```
$ cd movies-map
$ npm install
$ npm run start
```
That's all.
