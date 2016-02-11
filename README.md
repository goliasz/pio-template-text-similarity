# Text Similarity Based on Word2Vec

Text similarity engine based on Word2Vec algorithm. Builds vectors of full documents in training phase. Finds similar documents in query phase.

## Docker Part
```
docker pull goliasz/docker-predictionio
cd $HOME
mkdir MyEngine
docker run --hostname pio1 --privileged=true --name pio1 -it -p 8000:8000 -p 7070:7070 -p 7071:7071 -p 7072:7072 -v $HOME/MyEngine:/MyEngine goliasz docker-predictionio /bin/bash
```

## PIO Part
```
root@pio1:/# pio-start-all
root@pio1:/# cd MyEngine
root@pio1:/MyEngine# pio template get goliasz/pio-template-text-similarity --version "0.4" textsim
root@pio1:/MyEngine/textsim# vi engine.json
```

Set application name to “textsim”

```
root@pio1:/MyEngine/textsim# pio build --verbose
root@pio1:/MyEngine/textsim# pio app new textsim
root@pio1:/MyEngine/textsim# sh ./data/import_test.sh [YOUR APP ID from "pio app new textsim" output]
root@pio1:/MyEngine/textsim# pio train
root@pio1:/MyEngine/textsim# pio deploy --port 8000
```

## Test

### Event Server Status
```
curl -i -X GET http://localhost:7070
```

### Event Server: get all events 
```
curl -i -X GET http://localhost:7070/events.json?accessKey=[YOUR ACCESS KEY FROM "pio app new textsim" output]
```

### Query similarity score for a text a little bit similar to id:6
```
curl -X POST -H "Content-Type: application/json" -d '{"doc": "DJs flock by when MTV ax quiz prog. Five quacking zephyrs jolt my wax bed.", "limit", 3}' http://localhost:8000/queries.json
```

Result:
```
{"docScores":[{"score":0.2616939229262017,"id":"6","text":""},{"score":0.06153820944834193,"id":"3","text":""},{"score":-0.01023977852400198,"id":"2","text":""}]}
```

## License
This Software is licensed under the Apache Software Foundation version 2 licence found here: http://www.apache.org/licenses/LICENSE-2.0
