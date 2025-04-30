// KVManager.h
#ifndef KVMANAGER_H
#define KVMANAGER_H

#include <Arduino.h>
#include <vector>
#include <map>

struct KVPair {
    String key;
    String value;
};

class KVManager {
public:
    KVManager();
    void set(const String& key, const String& value);
    void markDirty(const String& key);
    std::vector<KVPair> getDirtyPairs();

private:
    std::map<String, String> kvStore;
    std::map<String, bool> dirtyFlags;
};

#endif // KVMANAGER_H
