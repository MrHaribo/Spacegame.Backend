docker run -p 61616:61616 --name couchbase --network mn_bridge_network --network-alias=couchbase couchbase