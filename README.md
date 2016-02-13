# Text Similarity Based on Word2Vec

Text similarity engine based on Word2Vec algorithm. Builds vectors of full documents in training phase. Finds similar documents in query phase.

## Model Training Modes

Similarity model can be trained using 2 sources of information.

Training modes are switched using engine.json configuration file. The change can’t be done using PubNub queues.

### Basic Training

Basic training is based only on "text" field from training event.
In this mode score equal 0 means that we are lacking training information for query phrase. Results and matches in such case are random. 

### Composite Training

In this mode we have two sources of information. We are concatenating fields "text" and "extTrainWords". This gives much more flexibility. If we have a case that we want to distinguish two quite similar phrases we can use second field to add additional desiding information allowing to match precisely and according to our needs.

Note that both fields can contain free texts. We don’t need to use single words there.

## Engine Configuration

File engine.json contains configuration of engine.

### minTokenSize

Only tokens of size equal and bigger than minTokenSize will be used to traing similarity model.

### showText

If set to true query result displays "text" field from training events.

### showDesc

If set to true query result displays "desc" field from training events.

### useExtTrainWords

If set to true composite training is used. Concatenated "text" and "extTrainWords" are source of training data. If set to false only "text" field is used as source of data for training.

### storeClearText

Default: false. If set to true all texts used for training are stored inside the model together with text vectors. Model is kept in memory. If training set is huge we are in danger of filling up significant portion of memory. If set to false model is very memory efficient. Only vectors of doubles are stored. By default vectors have 100 dimentions. This is configurable using "vectorSize". 

## Docker Part
```
docker pull goliasz/docker-predictionio
docker run --hostname tc1 --name tc1 -it goliasz/docker-predictionio /bin/bash
```

## PIO Part
```
root@tc1:/# pio-start-all
root@tc1:/# mkdir MyEngine
root@tc1:/# cd MyEngine
root@tc1:/MyEngine# pio template get goliasz/pio-template-text-similarity --version "0.7" textsim
root@tc1:/MyEngine# cd textsim
root@tc1:/MyEngine/textsim# vi engine.json
```

Set application name to “textsim”

```
root@pio1:/MyEngine/textsim# pio build --verbose
root@pio1:/MyEngine/textsim# pio app new textsim
root@pio1:/MyEngine/textsim# sh ./data/import_test.sh 1
root@pio1:/MyEngine/textsim# pio train
root@pio1:/MyEngine/textsim# pio deploy --port 8000 &
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

## License
This Software is licensed under the Apache Software Foundation version 2 licence found here: http://www.apache.org/licenses/LICENSE-2.0
