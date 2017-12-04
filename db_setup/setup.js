var dbName = "test";
var collectionName = "test";

db=db.getSiblingDB(dbName);
db[collectionName].createIndex({url:"text"});
db[collectionName].createIndex({processedDateTime:1},{expireAfterSeconds:604800});