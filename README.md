# StreamingClassifier

### Architecture:

![alt text](http://funkyimg.com/i/2tkym.png "SC Architecture")

### Sketch:

I. User wants to be able to automatically classify streaming data. Based on a category of a certain data point, user wants to redirect this datapoint to a certain place. Certain DB table or file. The resulting datapoint is original schema + class defined from the model. 
II. User has built a ML model prior. No training is involved. This is just a streaming classification. 
III. User knows the data shape of a data he wants to classify. If a data shape and a model don’t fit together, this should be handled.
IV. User knows the data sources he wants to connect to. He/she needs to have an ability to subscribe a data source to the application. Data source will be handled by a connector. A connector node can be placed anywhere in the cluster and close to the datasource to decrease overhead. Based on a data source type, a connector node should be able to use an appropriate alpaca module. If it’s a file, it’s a file connector. If it’s a Cassandra DB it’s a Cassandra connector, etc. 
V. Scalability options: Increase number of connectors to divide-and-conquer many streams (scale out) and/or increase number of connector children actors (scale up). For example, if a directory is shown as a source, create each actor per file (kinda like Spark does). Fan-in streams on a connector manager level or connector level. 
VI. It’s important to address all the streaming issues that might arise (fast producer - slow consumer, back-pressure, non-blocking io, etc) as well as distributed issues (fault tolerance, persistence, recovery, self-healing, etc). 
VII. User actions should be performed from a pretty one page UI. Several clients should be able to connect at the same time. The UI should support an active web socket connections, rudimentary analytics, user logging and all the goodies.

### Advanced:

Besides a subscription streaming, there should be an ability for streaming queries. Options: Apache Calcite, Spark DataFrames. 
